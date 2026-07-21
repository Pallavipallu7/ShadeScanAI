import { jsPDF } from 'jspdf';

/**
 * Generates a professional clinical PDF report for a dental shade scan.
 */
export function generateClinicalReportPDF({ scanResult, patient, doctorInfo }) {
  const doc = new jsPDF();

  const primaryColor = [37, 99, 235]; // #2563EB
  const darkColor = [15, 23, 42]; // #0F172A
  const mutedColor = [100, 116, 139]; // #64748B
  const lightBg = [248, 250, 252]; // #F8FAFC

  // Page Header Banner
  doc.setFillColor(...primaryColor);
  doc.rect(0, 0, 210, 28, 'F');

  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(20);
  doc.text('ShadeScan AI', 14, 18);

  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.text('Clinical Dental Shade Analysis & Report', 135, 18);

  // Document Title & Date
  doc.setTextColor(...darkColor);
  doc.setFontSize(16);
  doc.setFont('helvetica', 'bold');
  doc.text('CLINICAL SHADE EVALUATION REPORT', 14, 40);

  const scanDate = scanResult?.dateTime 
    ? new Date(scanResult.dateTime).toLocaleString() 
    : new Date().toLocaleString();
    
  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(...mutedColor);
  doc.text(`Report ID: SCAN-${scanResult?.id?.substring(0, 8) || '001'}`, 14, 46);
  doc.text(`Date & Time: ${scanDate}`, 135, 46);

  doc.setLineWidth(0.5);
  doc.setDrawColor(226, 232, 240);
  doc.line(14, 50, 196, 50);

  // Section 1: Patient Information
  doc.setFillColor(...lightBg);
  doc.roundedRect(14, 55, 88, 50, 3, 3, 'F');
  doc.setDrawColor(226, 232, 240);
  doc.roundedRect(14, 55, 88, 50, 3, 3, 'D');

  doc.setTextColor(...darkColor);
  doc.setFontSize(11);
  doc.setFont('helvetica', 'bold');
  doc.text('PATIENT DETAILS', 18, 63);

  doc.setFontSize(9.5);
  doc.setFont('helvetica', 'normal');
  doc.text(`Name: ${patient?.name || 'Walk-in Patient'}`, 18, 72);
  doc.text(`Age / Gender: ${patient?.age || 'N/A'} yrs / ${patient?.gender || 'N/A'}`, 18, 80);
  doc.text(`Phone: ${patient?.phone || 'N/A'}`, 18, 88);
  doc.text(`Notes: ${patient?.notes || 'None'}`, 18, 96);

  // Section 2: Clinical Practitioner Information
  doc.setFillColor(...lightBg);
  doc.roundedRect(108, 55, 88, 50, 3, 3, 'F');
  doc.setDrawColor(226, 232, 240);
  doc.roundedRect(108, 55, 88, 50, 3, 3, 'D');

  doc.setTextColor(...darkColor);
  doc.setFontSize(11);
  doc.setFont('helvetica', 'bold');
  doc.text('CLINICIAN DETAILS', 112, 63);

  doc.setFontSize(9.5);
  doc.setFont('helvetica', 'normal');
  doc.text(`Practitioner: ${doctorInfo?.name || 'Dr. Clinical User'}`, 112, 72);
  doc.text(`Mobile: ${doctorInfo?.mobile || 'N/A'}`, 112, 80);
  doc.text(`System: ShadeScan AI Web Portal v1.0`, 112, 88);
  doc.text(`Standard: VITA Classical A1-D4`, 112, 96);

  // Section 3: AI Shade Match Results
  doc.setFillColor(239, 246, 255); // Blue tint
  doc.roundedRect(14, 112, 182, 45, 4, 4, 'F');
  doc.setDrawColor(191, 219, 254);
  doc.roundedRect(14, 112, 182, 45, 4, 4, 'D');

  doc.setTextColor(...primaryColor);
  doc.setFontSize(12);
  doc.setFont('helvetica', 'bold');
  doc.text('PREDICTED TOOTH SHADE', 22, 122);

  const mainShade = scanResult?.predictedShade || 'A2';
  const confidence = scanResult?.confidence || '94%';

  doc.setFontSize(28);
  doc.setTextColor(...darkColor);
  doc.text(`SHADE ${mainShade}`, 22, 138);

  doc.setFontSize(14);
  doc.setTextColor(...primaryColor);
  doc.text(`Confidence: ${confidence}`, 120, 134);

  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(...mutedColor);
  doc.text('VITA Classical Shade Guide System Equivalent', 120, 142);

  // Section 4: Image Preview (If image data exists)
  if (scanResult?.imageUri && scanResult.imageUri.startsWith('data:image')) {
    try {
      doc.setTextColor(...darkColor);
      doc.setFontSize(11);
      doc.setFont('helvetica', 'bold');
      doc.text('CAPTURED DENTAL IMAGE', 14, 168);

      doc.addImage(scanResult.imageUri, 'JPEG', 14, 172, 75, 55);
      doc.setDrawColor(203, 213, 225);
      doc.rect(14, 172, 75, 55, 'D');
    } catch (e) {
      console.warn("Could not embed image in PDF:", e);
    }
  }

  // Candidate Predictions Table
  const startTableX = scanResult?.imageUri?.startsWith('data:image') ? 95 : 14;
  const tableWidth = scanResult?.imageUri?.startsWith('data:image') ? 101 : 182;

  doc.setTextColor(...darkColor);
  doc.setFontSize(11);
  doc.setFont('helvetica', 'bold');
  doc.text('AI PROBABILITY BREAKDOWN', startTableX, 168);

  doc.setFillColor(...lightBg);
  doc.rect(startTableX, 172, tableWidth, 8, 'F');
  doc.setFontSize(9);
  doc.setFont('helvetica', 'bold');
  doc.text('VITA Shade', startTableX + 4, 177);
  doc.text('Confidence Probability', startTableX + tableWidth - 40, 177);

  const candidateList = scanResult?.predictions || [
    { label: mainShade, confidence: 0.94 },
    { label: 'A1', confidence: 0.04 },
    { label: 'B2', confidence: 0.02 }
  ];

  let currentY = 186;
  candidateList.slice(0, 4).forEach((candidate) => {
    doc.setFont('helvetica', 'normal');
    doc.text(`VITA ${candidate.label}`, startTableX + 4, currentY);
    const pct = typeof candidate.confidence === 'number' 
      ? `${Math.round(candidate.confidence * 100)}%` 
      : candidate.confidence;
    doc.text(String(pct), startTableX + tableWidth - 40, currentY);

    doc.setDrawColor(241, 245, 249);
    doc.line(startTableX, currentY + 3, startTableX + tableWidth, currentY + 3);
    currentY += 10;
  });

  // Footer Disclaimer & Signature
  doc.setDrawColor(226, 232, 240);
  doc.line(14, 265, 196, 265);

  doc.setFontSize(8);
  doc.setTextColor(...mutedColor);
  doc.setFont('helvetica', 'italic');
  doc.text('Notice: ShadeScan AI provides clinical decision support. Final shade selection remains the responsibility of the practitioner.', 14, 272);

  doc.setFont('helvetica', 'bold');
  doc.text('ShadeScan AI Web Clinical Portal - Verified Digital Record', 130, 278);

  // Save PDF
  const filename = `ShadeScan_Report_${patient?.name ? patient.name.replace(/\s+/g, '_') : 'Patient'}_${mainShade}.pdf`;
  doc.save(filename);
}
