package com.example.gogo.history

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // 또는 Button (본인이 만든 버튼 종류에 맞게 import)
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gogo.R
import com.google.gson.Gson

@SuppressLint("SetTextI18n")
// 💡 리스트가 통째로 날아가서 텅 볐을 때 화면 처리를 위해 공백 리스너(onEmptyListener)를 추가했습니다.
class HistoryAdapter(
    private val historyList: ArrayList<GameHistory>,
    private val onEmptyListener: () -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvItemDate)
        val tvTime: TextView = view.findViewById(R.id.tvItemTime)
        val layoutPlayers: LinearLayout = view.findViewById(R.id.layoutItemPlayers)

        // 💡 본인이 xml에 만든 삭제 버튼 ID를 여기에 연결하세요! (id는 임의로 R.id.btnDeleteItem 로 가정함)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvDate.text = "📅 ${item.date}"
        holder.tvTime.text = "⏱️ ${item.totalGameTime}"

        holder.layoutPlayers.removeAllViews()

        for (player in item.players) {
            val playerTextView = TextView(holder.itemView.context).apply {
                textSize = 16f
                setPadding(0, 8, 0, 8)
                if (player.isWinner) {
                    text = "🏆 ${player.name} : ${player.finalScore}점"
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                } else {
                    text = "👤 ${player.name} : ${player.finalScore}점"
                    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
            }
            holder.layoutPlayers.addView(playerTextView)
        }

        // -------------------------------------------------------------
        // 🗑️ [삭제 버튼 클릭 이벤트 구현부]
        // -------------------------------------------------------------
        holder.btnDelete.setOnClickListener {
            val context = holder.itemView.context

            // 실수로 누르는 걸 방지하기 위해 더블 체크 팝업 띄우기
            AlertDialog.Builder(context).apply {
                setTitle("기록 삭제")
                setMessage("이 게임 기록을 정말로 삭제하시겠습니까?")

                setPositiveButton("삭제") { _, _ ->
                    // 1. 현재 누른 아이템의 최신 위치(인덱스)를 다시 안전하게 가져옴
                    val actualPosition = holder.adapterPosition
                    if (actualPosition != RecyclerView.NO_POSITION) {

                        // 2. 메모리 상의 리스트에서 데이터 제거
                        historyList.removeAt(actualPosition)

                        // 3. SharedPreferences에 변경된 전체 리스트를 다시 세이브(동기화)
                        saveUpdatedListToStorage(context)

                        // 4. 리사이클러뷰에 삭제 연출과 함께 새로고침 통보
                        notifyItemRemoved(actualPosition)
                        notifyItemRangeChanged(actualPosition, historyList.size)

                        // 5. 만약 지웠는데 남은 기록이 하나도 없다면 "기록이 없습니다" 문구를 띄우라고 액티비티에 신호 보냄
                        if (historyList.isEmpty()) {
                            onEmptyListener()
                        }
                    }
                }
                setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }

    override fun getItemCount(): Int = historyList.size

    /**
     * 💾 리스트에서 데이터가 삭제될 때마다 SharedPreferences를 최신화하는 함수
     */
    private fun saveUpdatedListToStorage(context: Context) {
        val sharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        // 변경된 리스트를 다시 JSON 글자로 인코딩
        val updatedJson = gson.toJson(historyList)
        // 덮어쓰기 저장
        sharedPreferences.edit().putString("history_list", updatedJson).apply()
    }
}
