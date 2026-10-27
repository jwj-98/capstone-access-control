package kr.ac.mjc.capstar

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LogAdapter(var context: Context, var logList: ArrayList<SpaceLog>): RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    var itemClickListener:ItemClickListener?=null
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var database: DatabaseReference

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var authCheckTv: TextView = itemView.findViewById(R.id.log_authCheck_tv)
        var time_tv: TextView = itemView.findViewById(R.id.log_date_tv)
        var authMethodTv: TextView = itemView.findViewById(R.id.log_authMethod_tv)
        var authNameTv: TextView = itemView.findViewById(R.id.log_name_tv)

        fun bind(log: SpaceLog){
            if(log.authCheck)authCheckTv.text = "성공"
            else authCheckTv.text = "실패"
            authMethodTv.text = log.authMethod
            time_tv.text = log.date
            var name: String = log.name_phoneNo.split("_")[0]
            if(name.length > 3){
                name = name.substring(0,3)
                name = name[0] + "*" + name[2] + ".."
                authNameTv.text = name
            }else if(name.length == 3){
                name = name[0] + "*" + name[2]
                authNameTv.text = name
            }else if(name.length == 2){
                name = name[0] + "*"
                authNameTv.text = name
            }else{
                authNameTv.text = name
            }

            itemView.setOnClickListener {
                if(!log.authCheck && log.authMethod.equals("지문인식"))
                else {
                    val intent = Intent(context, AuthImagePopup::class.java)
                    intent.putExtra(AuthImagePopup.INTENT_URL, log)
                    context.startActivity(intent)
                }


            }

            itemView.setOnLongClickListener {
                AlertDialog.Builder(context)
                        .setMessage("해당 출입기록을 삭제하시겠습니까?")
                        .setPositiveButton("취소", DialogInterface.OnClickListener { dialog, which -> })
                        .setNegativeButton("확인", DialogInterface.OnClickListener { dialog, which ->
                            firebaseDatabase = FirebaseDatabase.getInstance()
                            database = firebaseDatabase.getReference()

                            database.child(log.code).child("Log").orderByKey().equalTo(log.key)
                                    .addListenerForSingleValueEvent(object : ValueEventListener{
                                        override fun onCancelled(p0: DatabaseError) {
                                        }
                                        override fun onDataChange(p0: DataSnapshot) {
                                            p0.child(log.key).ref.removeValue()
                                        }
                                    })
                        })
                        .show()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(context).inflate(R.layout.log_activity, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return logList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(logList[position])
    }

    interface ItemClickListener{
        fun onLongClick(message: SpaceLog)
    }

}