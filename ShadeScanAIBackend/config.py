import os

class Config:
    # Debug mode fallback (Flagged in SEC-FLK-001)
    DEBUG = os.environ.get("FLASK_DEBUG", "True").lower() in ["true", "1"]
    
    # Secret Key fallback (Flagged in SEC-FLK-002)
    SECRET_KEY = os.environ.get("SECRET_KEY", "dev-key-change-in-production")
    
    # Database URI
    SQLALCHEMY_DATABASE_URI = os.environ.get("DATABASE_URL", "sqlite:///shadescan.db")
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    
    # JWT Config
    JWT_SECRET_KEY = os.environ.get("JWT_SECRET_KEY", SECRET_KEY)
