# Improve bodyweight movement tracking

Status: backlog idea

Reason:
- Pull-ups and similar bodyweight movements can currently present as if they have no weight, which makes it unclear whether the issue is demo data, configuration, or rendering.
- The movement being bodyweight-based is a separate fact from the athlete's actual body weight on that workout date.

Scope:
- Audit pull-ups and other bodyweight movements to confirm whether missing displayed weight is caused by dummy data, config, or software logic.
- Represent bodyweight movements explicitly instead of letting them appear blank.
- Capture the athlete's body weight at workout time so historical charts use the recorded value from that session, not the current settings value.

Implementation notes:
- Keep "bodyweight" as the movement/weight mode, and store workout-time body weight separately as a snapshot value.
- Decide whether the snapshot belongs on the session or each set based on how charts aggregate bodyweight movements.
- Make sure progress/history UI can explain bodyweight movements clearly, including cases where extra load is added later.
