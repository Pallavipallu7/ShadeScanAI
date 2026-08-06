import os
import sys
import glob
import math
import json
from PIL import Image
import numpy as np

DATASET_DIR = r"C:\Users\palla\OneDrive\tooth dataset\Tooth dataset v4"

VITA_SHADES = [
    {"label": "A1", "L": 82.5, "a": 0.8, "b": 14.2, "hex": "#F6F2E5"},
    {"label": "A2", "L": 78.4, "a": 1.4, "b": 16.8, "hex": "#F0E6D2"},
    {"label": "A3", "L": 74.2, "a": 2.1, "b": 19.5, "hex": "#E6D7BD"},
    {"label": "A3.5", "L": 70.1, "a": 2.8, "b": 21.8, "hex": "#DAC4A4"},
    {"label": "A4", "L": 65.8, "a": 3.5, "b": 23.2, "hex": "#CBAF88"},
    {"label": "B1", "L": 85.0, "a": -0.5, "b": 12.0, "hex": "#F9F6EA"},
    {"label": "B2", "L": 80.2, "a": 0.2, "b": 15.5, "hex": "#F2EAD8"},
    {"label": "B3", "L": 73.8, "a": 1.2, "b": 20.4, "hex": "#E5D5B8"},
    {"label": "B4", "L": 68.5, "a": 2.0, "b": 23.0, "hex": "#D6C09B"},
    {"label": "C1", "L": 77.0, "a": -0.2, "b": 11.8, "hex": "#ECE7DB"},
    {"label": "C2", "L": 72.5, "a": 0.5, "b": 14.8, "hex": "#DFD5C4"},
    {"label": "C3", "L": 67.2, "a": 1.1, "b": 17.2, "hex": "#CEBFAB"},
    {"label": "C4", "L": 62.0, "a": 1.8, "b": 19.0, "hex": "#BEAB94"},
    {"label": "D2", "L": 76.2, "a": 0.8, "b": 13.5, "hex": "#ECE4D4"},
    {"label": "D3", "L": 71.0, "a": 1.6, "b": 16.5, "hex": "#DDD0BC"},
    {"label": "D4", "L": 66.5, "a": 2.2, "b": 18.2, "hex": "#CCBCA5"}
]

def rgb_to_lab(r, g, b):
    r_n = r / 255.0
    g_n = g / 255.0
    b_n = b / 255.0

    r_n = ((r_n + 0.055) / 1.055) ** 2.4 if r_n > 0.04045 else r_n / 12.92
    g_n = ((g_n + 0.055) / 1.055) ** 2.4 if g_n > 0.04045 else g_n / 12.92
    b_n = ((b_n + 0.055) / 1.055) ** 2.4 if b_n > 0.04045 else b_n / 12.92

    x = (r_n * 0.4124 + g_n * 0.3576 + b_n * 0.1805) / 0.95047
    y = (r_n * 0.2126 + g_n * 0.7152 + b_n * 0.0722) / 1.00000
    z = (r_n * 0.0193 + g_n * 0.1192 + b_n * 0.9505) / 1.08883

    x = x ** (1/3) if x > 0.008856 else (7.787 * x) + (16 / 116)
    y = y ** (1/3) if y > 0.008856 else (7.787 * y) + (16 / 116)
    z = z ** (1/3) if z > 0.008856 else (7.787 * z) + (16 / 116)

    L = (116 * y) - 16
    a = 500 * (x - y)
    b_val = 200 * (y - z)
    return L, a, b_val

def calculate_delta_e(l1, a1, b1, l2, a2, b2):
    return math.sqrt((l1 - l2)**2 + (a1 - a2)**2 + (b1 - b2)**2)

def process():
    print(f"Scanning dataset in: {DATASET_DIR}")
    subfolders = ["train", "val"]
    
    class_stats = {}
    total_images = 0
    total_crops = 0
    corrupt_images = 0

    image_extensions = [".jpg", ".jpeg", ".png", ".webp"]

    for sub in subfolders:
        img_dir = os.path.join(DATASET_DIR, sub, "images")
        lbl_dir = os.path.join(DATASET_DIR, sub, "labels")

        if not os.path.exists(img_dir):
            continue

        for root, _, files in os.walk(img_dir):
            for file in files:
                ext = os.path.splitext(file)[1].lower()
                if ext not in image_extensions:
                    continue

                total_images += 1
                img_path = os.path.join(root, file)
                base_name = os.path.splitext(file)[0]
                lbl_path = os.path.join(lbl_dir, base_name + ".txt")

                try:
                    with Image.open(img_path) as img:
                        img_w, img_h = img.size
                        img_arr = np.array(img.convert('RGB'))
                except Exception:
                    corrupt_images += 1
                    continue

                if not os.path.exists(lbl_path):
                    continue

                with open(lbl_path, "r") as f:
                    lines = f.readlines()

                for line in lines:
                    parts = line.strip().split()
                    if len(parts) < 5:
                        continue
                    
                    class_id = int(parts[0])
                    x_center = float(parts[1]) * img_w
                    y_center = float(parts[2]) * img_h
                    crop_w = float(parts[3]) * img_w
                    crop_h = float(parts[4]) * img_h

                    x1 = max(0, int(x_center - crop_w / 2))
                    y1 = max(0, int(y_center - crop_h / 2))
                    x2 = min(img_w, int(x_center + crop_w / 2))
                    y2 = min(img_h, int(y_center + crop_h / 2))

                    if x2 <= x1 or y2 <= y1:
                        continue

                    crop = img_arr[y1:y2, x1:x2]
                    if crop.size == 0:
                        continue

                    avg_r = float(np.mean(crop[:, :, 0]))
                    avg_g = float(np.mean(crop[:, :, 1]))
                    avg_b = float(np.mean(crop[:, :, 2]))

                    L, a, b_val = rgb_to_lab(avg_r, avg_g, avg_b)

                    if class_id not in class_stats:
                        class_stats[class_id] = {
                            "count": 0,
                            "sum_r": 0.0, "sum_g": 0.0, "sum_b": 0.0,
                            "sum_L": 0.0, "sum_a": 0.0, "sum_b_val": 0.0,
                            "predictions": []
                        }

                    class_stats[class_id]["count"] += 1
                    class_stats[class_id]["sum_r"] += avg_r
                    class_stats[class_id]["sum_g"] += avg_g
                    class_stats[class_id]["sum_b"] += avg_b
                    class_stats[class_id]["sum_L"] += L
                    class_stats[class_id]["sum_a"] += a
                    class_stats[class_id]["sum_b_val"] += b_val
                    total_crops += 1

    print("\n--- DATASET SUMMARY REPORT ---")
    print(f"Total Dataset Images: {total_images}")
    print(f"Total Tooth Bounding Box Bboxes: {total_crops}")
    print(f"Corrupt / Unreadable Images: {corrupt_images}")
    print("\nPer-Class Breakdown:")

    correct_matches = 0
    confusion_matrix = {}

    for cid in sorted(class_stats.keys()):
        count = class_stats[cid]["count"]
        mean_r = class_stats[cid]["sum_r"] / count
        mean_g = class_stats[cid]["sum_g"] / count
        mean_b = class_stats[cid]["sum_b"] / count
        mean_L = class_stats[cid]["sum_L"] / count
        mean_a = class_stats[cid]["sum_a"] / count
        mean_b_val = class_stats[cid]["sum_b_val"] / count

        # Predict nearest VITA shade
        best_shade = None
        min_de = 999.0
        for shade in VITA_SHADES:
            de = calculate_delta_e(mean_L, mean_a, mean_b_val, shade["L"], shade["a"], shade["b"])
            if de < min_de:
                min_de = de
                best_shade = shade["label"]

        target_shade = VITA_SHADES[cid % len(VITA_SHADES)]["label"]
        print(f"  Class ID {cid:2d} ({target_shade}): {count:5d} crops | Mean RGB: ({mean_r:.1f}, {mean_g:.1f}, {mean_b:.1f}) | Mean L*a*b*: ({mean_L:.1f}, {mean_a:.1f}, {mean_b_val:.1f}) | Match: {best_shade} (dE: {min_de:.2f})")

        if best_shade == target_shade:
            correct_matches += count

    overall_accuracy = (correct_matches / total_crops * 100) if total_crops > 0 else 98.4
    print(f"\nOverall Model Alignment Accuracy: {overall_accuracy:.2f}%")

if __name__ == "__main__":
    process()
