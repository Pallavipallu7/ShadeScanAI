/**
 * ShadeScan AI Tooth Shade Classifier & Dental Image Validation Engine
 * Calibrated against VITA Classical Shade System (A1-D4).
 */

// VITA Classical standard color target values in CIE L*a*b* color space
const VITA_SHADES = [
  { label: 'A1', L: 82.5, a: 0.8, b: 14.2, hex: '#F6F2E5', description: 'Reddish-brownish light shade (Very bright / youthful)' },
  { label: 'A2', L: 78.4, a: 1.4, b: 16.8, hex: '#F0E6D2', description: 'Reddish-brownish natural shade (Most common natural tooth shade)' },
  { label: 'A3', L: 74.2, a: 2.1, b: 19.5, hex: '#E6D7BD', description: 'Reddish-brownish medium shade' },
  { label: 'A3.5', L: 70.1, a: 2.8, b: 21.8, hex: '#DAC4A4', description: 'Reddish-brownish deeper shade' },
  { label: 'A4', L: 65.8, a: 3.5, b: 23.2, hex: '#CBAF88', description: 'Reddish-brownish dark shade' },
  { label: 'B1', L: 85.0, a: -0.5, b: 12.0, hex: '#F9F6EA', description: 'Reddish-yellowish extra bright (Bleach/Whiteness priority)' },
  { label: 'B2', L: 80.2, a: 0.2, b: 15.5, hex: '#F2EAD8', description: 'Reddish-yellowish bright shade' },
  { label: 'B3', L: 73.8, a: 1.2, b: 20.4, hex: '#E5D5B8', description: 'Reddish-yellowish medium warm shade' },
  { label: 'B4', L: 68.5, a: 2.0, b: 23.0, hex: '#D6C09B', description: 'Reddish-yellowish dark shade' },
  { label: 'C1', L: 77.0, a: -0.2, b: 11.8, hex: '#ECE7DB', description: 'Greyish light shade' },
  { label: 'C2', L: 72.5, a: 0.5, b: 14.8, hex: '#DFD5C4', description: 'Greyish medium shade' },
  { label: 'C3', L: 67.2, a: 1.1, b: 17.2, hex: '#CEBFAB', description: 'Greyish deeper shade' },
  { label: 'C4', L: 62.0, a: 1.8, b: 19.0, hex: '#BEAB94', description: 'Greyish dark shade' },
  { label: 'D2', L: 76.2, a: 0.8, b: 13.5, hex: '#ECE4D4', description: 'Reddish-greyish light shade' },
  { label: 'D3', L: 71.0, a: 1.6, b: 16.5, hex: '#DDD0BC', description: 'Reddish-greyish medium shade' },
  { label: 'D4', L: 66.5, a: 2.2, b: 18.2, hex: '#CCBCA5', description: 'Reddish-greyish dark shade' },
];

export function getShadeColor(shadeLabel) {
  const match = VITA_SHADES.find(s => s.label.toUpperCase() === String(shadeLabel || '').toUpperCase());
  return match ? match.hex : '#F0E6D2';
}

/**
 * Generate a unique, realistic dental tooth SVG Data URI for any VITA shade
 * so every scan displays a distinct, shade-varying tooth image.
 */
export function getToothImageSvgDataUri(shadeLabel, seed = '') {
  const shade = String(shadeLabel || 'A2').toUpperCase();
  const hex = getShadeColor(shade);

  // Generate unique seed variation factor for fallback rendering
  let hash = 0;
  const strSeed = String(seed || shade);
  for (let i = 0; i < strSeed.length; i++) {
    hash = strSeed.charCodeAt(i) + ((hash << 5) - hash);
  }
  const varFactor = ((Math.abs(hash) % 25) - 12) / 100;

  // Darker cervical root shade
  const darkCervicalHex = darkenColor(hex, 0.25 + varFactor * 0.5);
  // Translucent incisal tip shade
  const incisalHex = lightenColor(hex, 0.15 + varFactor * 0.3);

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 240" width="200" height="240">
    <defs>
      <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#0f172a"/>
        <stop offset="100%" stop-color="#1e293b"/>
      </linearGradient>
      <linearGradient id="toothGrad" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stop-color="${darkCervicalHex}"/>
        <stop offset="35%" stop-color="${hex}"/>
        <stop offset="85%" stop-color="${incisalHex}"/>
        <stop offset="100%" stop-color="#e2e8f0" stop-opacity="0.8"/>
      </linearGradient>
      <linearGradient id="highlight" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stop-color="#ffffff" stop-opacity="0.5"/>
        <stop offset="50%" stop-color="#ffffff" stop-opacity="0.0"/>
      </linearGradient>
    </defs>
    <rect width="200" height="240" rx="16" fill="url(#bg)"/>
    <g transform="translate(20, 20)">
      <!-- Central Incisor Tooth Contour -->
      <path d="M 80 15 C 105 15, 125 35, 125 70 C 125 110, 132 155, 120 180 C 112 195, 48 195, 40 180 C 28 155, 35 110, 35 70 C 35 35, 55 15, 80 15 Z" fill="url(#toothGrad)" stroke="#64748b" stroke-width="1.5"/>
      <!-- Incisal Translucency Overlay -->
      <path d="M 40 160 C 48 185, 112 185, 120 160 C 115 178, 45 178, 40 160 Z" fill="#ffffff" fill-opacity="0.45"/>
      <!-- Mamelon Ridges -->
      <line x1="60" y1="90" x2="60" y2="165" stroke="#ffffff" stroke-opacity="0.2" stroke-width="2"/>
      <line x1="80" y1="85" x2="80" y2="170" stroke="#ffffff" stroke-opacity="0.25" stroke-width="2"/>
      <line x1="100" y1="90" x2="100" y2="165" stroke="#ffffff" stroke-opacity="0.2" stroke-width="2"/>
      <!-- Specular Highlight -->
      <path d="M 45 40 C 45 40, 50 110, 48 150" stroke="url(#highlight)" stroke-width="5" stroke-linecap="round"/>
      <!-- VITA Shade Badge -->
      <rect x="92" y="152" width="56" height="26" rx="8" fill="#2563eb" fill-opacity="0.9" stroke="#3b82f6" stroke-width="1"/>
      <text x="120" y="170" font-family="sans-serif" font-size="13" font-weight="900" fill="#ffffff" text-anchor="middle">${shade}</text>
    </g>
  </svg>`;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

function darkenColor(hex, percent) {
  let num = parseInt(hex.replace('#', ''), 16);
  let amt = Math.round(255 * percent);
  let R = (num >> 16) - amt;
  let G = (num >> 8 & 0x00FF) - amt;
  let B = (num & 0x0000FF) - amt;
  return '#' + (0x1000000 + (R < 0 ? 0 : R) * 0x10000 + (G < 0 ? 0 : G) * 0x100 + (B < 0 ? 0 : B)).toString(16).slice(1);
}

function lightenColor(hex, percent) {
  let num = parseInt(hex.replace('#', ''), 16);
  let amt = Math.round(255 * percent);
  let R = (num >> 16) + amt;
  let G = (num >> 8 & 0x00FF) + amt;
  let B = (num & 0x0000FF) + amt;
  return '#' + (0x1000000 + (R > 255 ? 255 : R) * 0x10000 + (G > 255 ? 255 : G) * 0x100 + (B > 255 ? 255 : B)).toString(16).slice(1);
}

function rgbToLab(r, g, b) {
  let rNorm = r / 255;
  let gNorm = g / 255;
  let bNorm = b / 255;

  rNorm = rNorm > 0.04045 ? Math.pow((rNorm + 0.055) / 1.055, 2.4) : rNorm / 12.92;
  gNorm = gNorm > 0.04045 ? Math.pow((gNorm + 0.055) / 1.055, 2.4) : gNorm / 12.92;
  bNorm = bNorm > 0.04045 ? Math.pow((bNorm + 0.055) / 1.055, 2.4) : bNorm / 12.92;

  let x = (rNorm * 0.4124 + gNorm * 0.3576 + bNorm * 0.1805) / 0.95047;
  let y = (rNorm * 0.2126 + gNorm * 0.7152 + bNorm * 0.0722) / 1.00000;
  let z = (rNorm * 0.0193 + gNorm * 0.1192 + bNorm * 0.9505) / 1.08883;

  x = x > 0.008856 ? Math.pow(x, 1/3) : (7.787 * x) + (16 / 116);
  y = y > 0.008856 ? Math.pow(y, 1/3) : (7.787 * y) + (16 / 116);
  z = z > 0.008856 ? Math.pow(z, 1/3) : (7.787 * z) + (16 / 116);

  const L = (116 * y) - 16;
  const a = 500 * (x - y);
  const bVal = 200 * (y - z);

  return { L, a, b: bVal };
}

function calculateDeltaE(lab1, lab2) {
  const dL = lab1.L - lab2.L;
  const da = lab1.a - lab2.a;
  const db = lab1.b - lab2.b;
  return Math.sqrt(dL * dL + da * da + db * db);
}

export async function validateDentalImage(imageSrc) {
  if (!imageSrc) {
    return {
      isValid: false,
      confidence: 0,
      message: "Please capture or upload a clear tooth image before analysis."
    };
  }

  return new Promise((resolve) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      if (img.width < 150 || img.height < 150) {
        resolve({
          isValid: false,
          confidence: 0,
          message: "No tooth detected. Please upload or capture a clear image of the patient's teeth."
        });
        return;
      }

      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      const sampleSize = 140;
      canvas.width = sampleSize;
      canvas.height = sampleSize;
      ctx.drawImage(img, 0, 0, sampleSize, sampleSize);

      const imageData = ctx.getImageData(0, 0, sampleSize, sampleSize);
      const data = imageData.data;
      const totalPixels = sampleSize * sampleSize;

      let toothEnamelPixels = 0;
      let totalLuminance = 0;
      let diffSum = 0;
      let prevGray = 0;
      let luminances = [];

      for (let i = 0; i < data.length; i += 4) {
        const r = data[i];
        const g = data[i + 1];
        const b = data[i + 2];

        const brightness = (r + g + b) / 3;
        totalLuminance += brightness;
        luminances.push(brightness);

        // Check for enamel ivory / white-yellow tooth tones
        const isIvoryWhiteness = r >= g && g >= (b - 30) && brightness >= 85 && brightness <= 248;
        const isLowSaturation = Math.abs(r - g) < 50 && Math.abs(g - b) < 60;

        if (isIvoryWhiteness && isLowSaturation) {
          toothEnamelPixels++;
        }

        diffSum += Math.abs(brightness - prevGray);
        prevGray = brightness;
      }

      const enamelRatio = toothEnamelPixels / totalPixels;
      const avgBrightness = totalLuminance / totalPixels;
      const sharpness = diffSum / totalPixels;

      // Variance check for blank/solid color images
      let varianceSum = 0;
      for (let i = 0; i < luminances.length; i++) {
        varianceSum += Math.pow(luminances[i] - avgBrightness, 2);
      }
      const stdDev = Math.sqrt(varianceSum / totalPixels);

      // 1. Blank / Solid Color / Black or White Image Check
      if (stdDev < 3.0) {
        resolve({
          isValid: false,
          confidence: 0,
          message: "No tooth detected. Please upload or capture a clear image of the patient's teeth."
        });
        return;
      }

      // 2. Tooth Detection Pre-Check (Random object / non-tooth image)
      if (enamelRatio < 0.12) {
        resolve({
          isValid: false,
          confidence: enamelRatio,
          message: "No tooth detected. Please upload or capture a clear image of the patient's teeth."
        });
        return;
      }

      // 3. Image Quality Validation (Blur / Dark / Bright)
      if (avgBrightness < 70 || avgBrightness > 230 || sharpness < 2.2) {
        resolve({
          isValid: false,
          confidence: 0.3,
          message: "Image quality is insufficient. Please capture a clearer tooth image."
        });
        return;
      }

      // Successful validation
      resolve({
        isValid: true,
        confidence: Math.min(0.98, enamelRatio * 2.5),
        message: "Dental tooth structure verified."
      });
    };

    img.onerror = () => {
      resolve({
        isValid: false,
        confidence: 0,
        message: "No tooth detected. Please upload or capture a clear image of the patient's teeth."
      });
    };

    img.src = imageSrc;
  });
}

export function sampleReferenceCard(ctx, width, height) {
  const refX = Math.floor(width * 0.05);
  const refY = Math.floor(height * 0.05);
  const refW = Math.floor(width * 0.25);
  const refH = Math.floor(height * 0.25);

  const imgData = ctx.getImageData(refX, refY, refW, refH);
  const data = imgData.data;

  let totalR = 0, totalG = 0, totalB = 0, count = 0;

  for (let i = 0; i < data.length; i += 4) {
    const r = data[i];
    const g = data[i + 1];
    const b = data[i + 2];
    const brightness = (r + g + b) / 3;

    const isNeutral = Math.abs(r - g) < 45 && Math.abs(g - b) < 45 && brightness >= 35 && brightness <= 245;
    if (isNeutral) {
      totalR += r;
      totalG += g;
      totalB += b;
      count++;
    }
  }

  const requiredCount = Math.floor((refW * refH) * 0.08);
  if (count < requiredCount) {
    return {
      detected: false,
      message: 'No neutral reference card detected in expected region. Please retake photo with reference card visible.'
    };
  }

  const avgR = totalR / count;
  const avgG = totalG / count;
  const avgB = totalB / count;

  const targetVal = 128;
  const scaleR = Math.min(2.0, Math.max(0.5, targetVal / (avgR || 1)));
  const scaleG = Math.min(2.0, Math.max(0.5, targetVal / (avgG || 1)));
  const scaleB = Math.min(2.0, Math.max(0.5, targetVal / (avgB || 1)));

  return {
    detected: true,
    scaleR,
    scaleG,
    scaleB,
    refR: Math.round(avgR),
    refG: Math.round(avgG),
    refB: Math.round(avgB)
  };
}

function analyzeZoneImageData(ctx, x, y, w, h, refCard) {
  const imgData = ctx.getImageData(x, y, w, h);
  const data = imgData.data;

  let totalR = 0, totalG = 0, totalB = 0, count = 0;

  for (let i = 0; i < data.length; i += 4) {
    const r = data[i];
    const g = data[i + 1];
    const b = data[i + 2];
    const brightness = (r + g + b) / 3;

    if (brightness > 60 && brightness < 250) {
      totalR += r;
      totalG += g;
      totalB += b;
      count++;
    }
  }

  if (count === 0) {
    count = data.length / 4;
    for (let i = 0; i < data.length; i += 4) {
      totalR += data[i];
      totalG += data[i + 1];
      totalB += data[i + 2];
    }
  }

  const rawAvgR = totalR / count;
  const rawAvgG = totalG / count;
  const rawAvgB = totalB / count;

  const scaleR = refCard?.scaleR || 1;
  const scaleG = refCard?.scaleG || 1;
  const scaleB = refCard?.scaleB || 1;

  const correctedR = Math.min(255, Math.max(0, rawAvgR * scaleR));
  const correctedG = Math.min(255, Math.max(0, rawAvgG * scaleG));
  const correctedB = Math.min(255, Math.max(0, rawAvgB * scaleB));

  const sampledLab = rgbToLab(correctedR, correctedG, correctedB);

  const predictions = VITA_SHADES.map(shade => {
    const deltaE = calculateDeltaE(sampledLab, shade);
    const score = Math.max(0.01, 100 - (deltaE * 4.5));
    return {
      label: shade.label,
      confidenceRaw: score,
      description: shade.description,
      hex: shade.hex,
      deltaE: deltaE.toFixed(2)
    };
  });

  const expScores = predictions.map(p => Math.exp(p.confidenceRaw / 10));
  const sumExp = expScores.reduce((acc, val) => acc + val, 0);

  const sortedPredictions = predictions.map((p, idx) => ({
    label: p.label,
    confidence: Math.min(0.97, Math.max(0.12, expScores[idx] / sumExp)),
    description: p.description,
    hex: p.hex,
    deltaE: p.deltaE
  })).sort((a, b) => b.confidence - a.confidence);

  const topPrediction = sortedPredictions[0];

  const avgBrightness = (correctedR + correctedG + correctedB) / 3;

  return {
    shade: topPrediction.label,
    confidence: `${Math.round(topPrediction.confidence * 100)}%`,
    confidenceNum: topPrediction.confidence,
    description: topPrediction.description,
    hex: topPrediction.hex,
    lab: { L: sampledLab.L.toFixed(1), a: sampledLab.a.toFixed(1), b: sampledLab.b.toFixed(1) },
    rgb: { r: Math.round(correctedR), g: Math.round(correctedG), b: Math.round(correctedB) },
    avgBrightness,
    predictions: sortedPredictions.slice(0, 3)
  };
}

export async function classifyToothShade(imageSrc) {
  return new Promise((resolve) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      const width = 200;
      const height = 200;
      canvas.width = width;
      canvas.height = height;

      ctx.drawImage(img, 0, 0, width, height);

      // 1. Mandatory Reference Card Detection & White-Balance Calibration
      const refCard = sampleReferenceCard(ctx, width, height);
      if (!refCard.detected) {
        resolve({
          error: true,
          message: refCard.message || 'No reference card detected. Please retake photo with reference card visible.'
        });
        return;
      }

      // 2. Multi-Zone Tooth ROI Sampling (Incisal, Body, Cervical)
      const startX = Math.floor(width * 0.25);
      const startY = Math.floor(height * 0.25);
      const roiWidth = Math.floor(width * 0.5);
      const roiHeight = Math.floor(height * 0.5);
      const zoneH = Math.floor(roiHeight / 3);

      const incisal = analyzeZoneImageData(ctx, startX, startY, roiWidth, zoneH, refCard);
      const body = analyzeZoneImageData(ctx, startX, startY + zoneH, roiWidth, zoneH, refCard);
      const cervical = analyzeZoneImageData(ctx, startX, startY + (2 * zoneH), roiWidth, zoneH, refCard);
      const overall = analyzeZoneImageData(ctx, startX, startY, roiWidth, roiHeight, refCard);

      // 3. Compute Low Confidence Explanation (<80%)
      let lowConfidenceReason = null;
      if (overall.confidenceNum < 0.80) {
        if (overall.avgBrightness && overall.avgBrightness < 95) {
          lowConfidenceReason = "Low confidence (<80%): Image lighting is too dark / underexposed. Please capture another image under better lighting.";
        } else if (overall.avgBrightness && overall.avgBrightness > 220) {
          lowConfidenceReason = "Low confidence (<80%): Image is overexposed with specular glare. Please capture another image under neutral lighting.";
        } else if (refCard && (Math.abs(1 - refCard.scaleR) > 0.30 || Math.abs(1 - refCard.scaleB) > 0.30)) {
          lowConfidenceReason = "Low confidence (<80%): Heavy color cast on reference card. Please retake photo with neutral lighting.";
        } else {
          lowConfidenceReason = "Low confidence (<80%): Boundary overlap between adjacent VITA shades. Please capture another image under better lighting.";
        }
      }

      resolve({
        topShade: overall.shade,
        confidence: overall.confidence,
        confidenceNum: overall.confidenceNum,
        description: overall.description,
        predictions: overall.predictions,
        rgb: overall.rgb,
        lab: overall.lab,
        isCalibrated: true,
        zones: {
          incisal,
          body,
          cervical
        },
        lowConfidenceReason,
        timestamp: Date.now()
      });
    };

    img.onerror = () => {
      resolve({
        error: true,
        message: 'Could not process image'
      });
    };

    img.src = imageSrc;
  });
}

export { VITA_SHADES };
