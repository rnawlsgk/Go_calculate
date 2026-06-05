package com.example.gogo.history

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.gogo.R

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmptyHistory = findViewById<TextView>(R.id.tvEmptyHistory)

        btnBack.setOnClickListener { finish() }

        // 1. SharedPreferences 에서 JSON 데이터 읽어오기
        val sharedPreferences = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val jsonHistory = sharedPreferences.getString("history_list", null)

        // 2. 파싱 및 리스트 변환
        val gson = Gson()
        val historyList: List<GameHistory>? = if (jsonHistory != null) {
            val type = object : TypeToken<List<GameHistory>>() {}.type
            gson.fromJson(jsonHistory, type)
        } else {
            null
        }

        // 3. 리사이클러뷰 세팅 및 예외 처리
        if (historyList.isNullOrEmpty()) {
            tvEmptyHistory.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmptyHistory.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE

            // 리사이클러뷰 방향 설정 (세로 배치)
            rvHistory.layoutManager = LinearLayoutManager(this)
            // 어댑터 연결
            rvHistory.adapter = HistoryAdapter(ArrayList(historyList)) {
                // 아이템들을 지우다가 마지막 한 개까지 다 지워져서 완전히 텅 비게 되면 실행되는 블록입니다.
                tvEmptyHistory.visibility = View.VISIBLE
                rvHistory.visibility = View.GONE
            }
        }
    }
}