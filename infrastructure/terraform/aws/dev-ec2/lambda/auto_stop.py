import boto3
import os
from datetime import datetime, timezone


def handler(event, context):
    instance_id = os.environ["INSTANCE_ID"]
    max_hours = float(os.environ.get("MAX_RUNTIME_HOURS", "2"))

    ec2 = boto3.client("ec2")

    resp = ec2.describe_instances(InstanceIds=[instance_id])
    if not resp["Reservations"]:
        print(f"Instance {instance_id} not found")
        return {"stopped": False, "reason": "not_found"}

    instance = resp["Reservations"][0]["Instances"][0]
    state = instance["State"]["Name"]

    if state != "running":
        print(f"Instance is '{state}', nothing to do")
        return {"stopped": False, "reason": state}

    launch_time = instance["LaunchTime"]
    runtime_h = (datetime.now(timezone.utc) - launch_time).total_seconds() / 3600

    print(f"Instance {instance_id}: running {runtime_h:.2f}h (limit {max_hours}h)")

    if runtime_h >= max_hours:
        ec2.stop_instances(InstanceIds=[instance_id])
        print(f"STOPPED: runtime {runtime_h:.2f}h exceeded limit of {max_hours}h")
        return {"stopped": True, "runtime_hours": round(runtime_h, 2)}

    remaining = max_hours - runtime_h
    print(f"OK: {remaining:.2f}h remaining before auto-stop")
    return {"stopped": False, "runtime_hours": round(runtime_h, 2)}
