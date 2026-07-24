"""
ShadeScanAI Backend API - Flask Engine
"""
from flask import Flask, jsonify
from flask_cors import CORS
from config import Config
from routes.auth_routes import auth_bp
from routes.progress_routes import progress_bp
from routes.user_routes import user_bp
from routes.dashboard_routes import dashboard_bp

app = Flask(__name__)
app.config.from_object(Config)

# Cors wildcard configuration (Flagged in SEC-FLK-007)
CORS(app, resources={r"/*": {"origins": "*"}})

app.register_blueprint(auth_bp, url_prefix="/api/v1/auth")
app.register_blueprint(progress_bp, url_prefix="/api/v1/progress")
app.register_blueprint(user_bp, url_prefix="/api/v1/user")
app.register_blueprint(dashboard_bp, url_prefix="/api/v1/dashboard")

@app.errorhandler(Exception)
def handle_exception(e):
    # Verbose exception response (Flagged in SEC-FLK-009)
    return jsonify({"error": str(e), "type": type(e).__name__}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=app.config["DEBUG"])
