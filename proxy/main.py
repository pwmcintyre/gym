"""
Gym Whiteboard Proxy — minimal FastAPI service.

Receives a JPEG image from the Android app, forwards it to OpenAI Vision,
and returns structured workout JSON.

Deploy to Cloud Run:
  gcloud run deploy gym-proxy \
    --source . \
    --region us-central1 \
    --allow-unauthenticated \
    --set-env-vars OPENAI_API_KEY=sk-...

Environment variables:
  OPENAI_API_KEY   (required) OpenAI API key
  MAX_IMAGE_BYTES  (optional, default 4194304 = 4MB) request size limit
"""

import base64
import json
import os

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse
from openai import AsyncOpenAI

app = FastAPI(title="gym-proxy")

client = AsyncOpenAI(api_key=os.environ["OPENAI_API_KEY"])

MAX_IMAGE_BYTES = int(os.environ.get("MAX_IMAGE_BYTES", 4 * 1024 * 1024))

SYSTEM_PROMPT = """
You are a gym workout parser. The user will show you a photo of a gym whiteboard
or workout card. Extract the workout into structured JSON.

Return ONLY valid JSON matching this schema — no markdown, no explanation:
{
  "workout_date": null,
  "items": [
    {
      "label": "A1",
      "exercise_name": "Back Squat",
      "target_sets": 4,
      "target_reps": 8,
      "rep_modifier": "NONE",
      "notes": "3s pause at bottom",
      "raw_source_text": "A1 Back Squat 4 x 8 3s pause at bottom"
    }
  ]
}

Rules:
- label: the workout label (A1, B2, etc.) or empty string if absent
- exercise_name: the exercise name, cleaned up
- target_sets: integer or null
- target_reps: integer or null — for ranges like "3-4", use the higher number (4)
- rep_modifier: "NONE" or "MAX" — use "MAX" when the prescription says "max" or "amrap"
- notes: any extra instructions (tempo, rest, etc.) or empty string
- raw_source_text: the original text from the whiteboard for this item
- workout_date: ISO date string if visible on the board, otherwise null

If you cannot read the board or it is not a workout, return {"workout_date": null, "items": []}.
""".strip()


@app.post("/parse-workout")
async def parse_workout(image: UploadFile = File(...)) -> JSONResponse:
    data = await image.read()
    if len(data) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="Image too large")

    b64 = base64.standard_b64encode(data).decode()
    media_type = image.content_type or "image/jpeg"

    response = await client.chat.completions.create(
        model="gpt-4o",
        max_tokens=1024,
        messages=[
            {
                "role": "system",
                "content": SYSTEM_PROMPT,
            },
            {
                "role": "user",
                "content": [
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:{media_type};base64,{b64}", "detail": "high"},
                    },
                    {"type": "text", "text": "Parse this workout board."},
                ],
            },
        ],
    )

    raw = response.choices[0].message.content or ""
    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError:
        # Try stripping markdown code fences if model wrapped the JSON
        stripped = raw.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        try:
            parsed = json.loads(stripped)
        except json.JSONDecodeError:
            raise HTTPException(status_code=502, detail=f"Model returned non-JSON: {raw[:200]}")

    return JSONResponse(content=parsed)


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}
