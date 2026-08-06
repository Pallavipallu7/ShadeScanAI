import { 
  rtdb, 
  ref, 
  get, 
  set, 
  push, 
  update, 
  remove, 
  child 
} from '../firebase/config';

/**
 * Fetch Patients for the authenticated doctor from Firebase Realtime Database
 */
export async function getPatients(userId) {
  if (!userId || !rtdb) return [];
  try {
    const dbRef = ref(rtdb);
    const snapshot = await get(child(dbRef, `Patients/${userId}`));
    if (snapshot.exists()) {
      const data = snapshot.val();
      const list = [];
      Object.keys(data).forEach((key) => {
        list.push({
          id: key,
          name: data[key].name || 'Unknown Patient',
          age: data[key].age || '',
          gender: data[key].gender || '',
          phone: data[key].phone || '',
          notes: data[key].notes || '',
          createdAt: data[key].timestamp || Date.now()
        });
      });
      return list.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
    }
  } catch (error) {
    console.error("Error fetching Patients from Firebase Realtime DB:", error);
  }
  return [];
}

/**
 * Save new Patient to Firebase Realtime Database
 */
export async function savePatient(patientData, userId) {
  if (!userId || !rtdb) return null;
  try {
    const patientsRef = ref(rtdb, `Patients/${userId}`);
    const newPatientRef = push(patientsRef);
    const payload = {
      name: patientData.name,
      age: patientData.age || '',
      gender: patientData.gender || 'Female',
      phone: patientData.phone || '',
      notes: patientData.notes || '',
      timestamp: Date.now()
    };
    await set(newPatientRef, payload);
    return { id: newPatientRef.key, ...payload };
  } catch (error) {
    console.error("Error saving Patient to Firebase Realtime DB:", error);
    throw error;
  }
}

/**
 * Update Patient in Firebase Realtime Database
 */
export async function updatePatient(patientId, updatedFields, userId) {
  if (!userId || !patientId || !rtdb) return;
  try {
    const patientRef = ref(rtdb, `Patients/${userId}/${patientId}`);
    await update(patientRef, {
      name: updatedFields.name,
      age: updatedFields.age || '',
      gender: updatedFields.gender || 'Female',
      phone: updatedFields.phone || '',
      notes: updatedFields.notes || ''
    });
  } catch (error) {
    console.error("Error updating Patient in Firebase Realtime DB:", error);
    throw error;
  }
}

/**
 * Fetch Scan Reports for the authenticated doctor from Firebase Realtime Database
 */
export async function getScanHistory(userId) {
  if (!userId || !rtdb) return [];
  try {
    const dbRef = ref(rtdb);
    const snapshot = await get(child(dbRef, `Reports/${userId}`));
    if (snapshot.exists()) {
      const data = snapshot.val();
      const list = [];
      Object.keys(data).forEach((key) => {
        const item = data[key];
        const img = item.imageUri || item.image || item.photoUrl || item.capturedUri || null;
        list.push({
          id: key,
          patientId: item.patientId || 'walkin',
          patientName: item.patientName || 'Walk-in Patient',
          predictedShade: item.shade || item.predictedShade || 'A2',
          confidence: item.confidence || '94%',
          dateTime: item.timestamp || Date.now(),
          imageUri: img,
          predictions: item.predictions || [
            { label: item.shade || 'A2', confidence: 0.94, description: 'Matched shade' }
          ]
        });
      });
      return list.sort((a, b) => (b.dateTime || 0) - (a.dateTime || 0));
    }
  } catch (error) {
    console.error("Error fetching Reports from Firebase Realtime DB:", error);
  }
  return [];
}

/**
 * Save Scan Report to Firebase Realtime Database
 */
export async function saveScanReport(scanData, userId) {
  if (!userId || !rtdb) return null;
  try {
    const reportsRef = ref(rtdb, `Reports/${userId}`);
    const newReportRef = push(reportsRef);
    const payload = {
      patientId: scanData.patientId || '',
      patientName: scanData.patientName || 'Walk-in Patient',
      shade: scanData.predictedShade,
      confidence: scanData.confidence,
      timestamp: Date.now(),
      imageUri: scanData.imageUri || null,
      predictions: scanData.predictions || []
    };
    await set(newReportRef, payload);
    return { id: newReportRef.key, ...payload };
  } catch (error) {
    console.error("Error saving Report to Firebase Realtime DB:", error);
    throw error;
  }
}

/**
 * Soft Delete Scan Report to Recycle Bin (DeletedReports in Firebase / LocalStorage)
 */
export async function deleteScanReport(scanId, userId) {
  if (!userId || !scanId || !rtdb) return;
  try {
    const reportRef = ref(rtdb, `Reports/${userId}/${scanId}`);
    const snapshot = await get(reportRef);
    if (snapshot.exists()) {
      const data = snapshot.val();
      const deletedRef = ref(rtdb, `DeletedReports/${userId}/${scanId}`);
      await set(deletedRef, { ...data, deletedAt: Date.now() });
      await remove(reportRef);
    }
  } catch (error) {
    console.error("Error soft deleting Report from Firebase Realtime DB:", error);
    throw error;
  }
}

/**
 * Fetch Deleted Scan Reports (Recycle Bin) from Firebase Realtime Database
 */
export async function getDeletedScanHistory(userId) {
  if (!userId || !rtdb) return [];
  try {
    const dbRef = ref(rtdb);
    const snapshot = await get(child(dbRef, `DeletedReports/${userId}`));
    if (snapshot.exists()) {
      const data = snapshot.val();
      const list = [];
      Object.keys(data).forEach((key) => {
        const item = data[key];
        const img = item.imageUri || item.image || item.photoUrl || item.capturedUri || null;
        list.push({
          id: key,
          patientId: item.patientId || 'walkin',
          patientName: item.patientName || 'Walk-in Patient',
          predictedShade: item.shade || item.predictedShade || 'A2',
          confidence: item.confidence || '94%',
          dateTime: item.timestamp || Date.now(),
          deletedAt: item.deletedAt || Date.now(),
          imageUri: img,
          predictions: item.predictions || []
        });
      });
      return list.sort((a, b) => (b.deletedAt || 0) - (a.deletedAt || 0));
    }
  } catch (error) {
    console.error("Error fetching DeletedReports from Firebase Realtime DB:", error);
  }
  return [];
}

/**
 * Restore Scan Report from Recycle Bin
 */
export async function restoreScanReport(scanId, userId) {
  if (!userId || !scanId || !rtdb) return;
  try {
    const deletedRef = ref(rtdb, `DeletedReports/${userId}/${scanId}`);
    const snapshot = await get(deletedRef);
    if (snapshot.exists()) {
      const data = snapshot.val();
      const { deletedAt, ...reportData } = data;
      const reportRef = ref(rtdb, `Reports/${userId}/${scanId}`);
      await set(reportRef, reportData);
      await remove(deletedRef);
    }
  } catch (error) {
    console.error("Error restoring Report from Firebase Realtime DB:", error);
    throw error;
  }
}

/**
 * Permanently Delete Scan Report from Recycle Bin
 */
export async function permanentlyDeleteScanReport(scanId, userId) {
  if (!userId || !scanId || !rtdb) return;
  try {
    const deletedRef = ref(rtdb, `DeletedReports/${userId}/${scanId}`);
    await remove(deletedRef);
  } catch (error) {
    console.error("Error permanently deleting Report from Firebase Realtime DB:", error);
    throw error;
  }
}

/**
 * Empty Recycle Bin (Delete All)
 */
export async function deleteAllDeletedScanReports(userId) {
  if (!userId || !rtdb) return;
  try {
    const deletedRef = ref(rtdb, `DeletedReports/${userId}`);
    await remove(deletedRef);
  } catch (error) {
    console.error("Error emptying DeletedReports in Firebase Realtime DB:", error);
    throw error;
  }
}
