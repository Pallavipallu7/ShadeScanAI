package com.example.dental_shade_app.network

import com.example.dental_shade_app.models.ShadeScan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseManager {
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun getDoctorId(): String? = auth.currentUser?.uid

    fun saveScan(scan: ShadeScan, onComplete: (Boolean) -> Unit) {
        val doctorId = getDoctorId() ?: return onComplete(false)
        val scanId = database.child("doctors").child(doctorId).child("scans").push().key ?: return
        
        val scanWithId = scan.copy(id = scanId)
        database.child("doctors").child(doctorId).child("scans").child(scanId)
            .setValue(scanWithId)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun listenToStats(onUpdate: (patientCount: Int, scanCount: Int) -> Unit) {
        val doctorId = getDoctorId() ?: return
        
        database.child("doctors").child(doctorId).child("scans")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val scanCount = snapshot.childrenCount.toInt()
                    // Simplified: counting unique patient names as patient count
                    val patients = mutableSetOf<String>()
                    snapshot.children.forEach {
                        val scan = it.getValue(ShadeScan::class.java)
                        scan?.patientName?.let { name -> patients.add(name) }
                    }
                    onUpdate(patients.size, scanCount)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
