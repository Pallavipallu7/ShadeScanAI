from flask import Blueprint, jsonify

user_bp = Blueprint("user", __name__)

@user_bp.route("/profile", methods=["GET"])
def get_profile():
    return jsonify({"email": "user@shadescan.ai", "role": "dentist"})

@user_bp.route("/update", methods=["PUT"])
def update_profile():
    return jsonify({"status": "updated"})
