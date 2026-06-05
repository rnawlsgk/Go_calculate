package com.example.gogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gogo.history.HistoryActivity

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // 1. XML 레이아웃의 뷰(View)들을 가져옵니다.
        val rgPlayerCount = findViewById<RadioGroup>(R.id.rgPlayerCount)
        val etPlayer1 = findViewById<EditText>(R.id.etPlayer1)
        val etPlayer2 = findViewById<EditText>(R.id.etPlayer2)
        val etPlayer3 = findViewById<EditText>(R.id.etPlayer3)
        val etPlayer4 = findViewById<EditText>(R.id.etPlayer4)
        val btnStartGame = findViewById<Button>(R.id.btnStartGame)
        val btnHistories = findViewById<Button>(R.id.btnHistories)

        // 초기 상태 설정: 기본이 3인 플레이이므로 4번째 이름 입력창은 숨깁니다.
        etPlayer4.visibility = View.GONE

        // 2. 라디오 버튼 선택 변경 리스너 (인원수에 따라 입력창 보이기/숨기기)
        rgPlayerCount.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb2Players -> {
                    etPlayer3.visibility = View.GONE
                    etPlayer4.visibility = View.GONE
                }
                R.id.rb3Players -> {
                    etPlayer3.visibility = View.VISIBLE
                    etPlayer4.visibility = View.GONE
                }
                R.id.rb4Players -> {
                    etPlayer3.visibility = View.VISIBLE
                    etPlayer4.visibility = View.VISIBLE
                }
            }
        }

        // 3. 게임 시작 버튼 클릭 리스너
        btnStartGame.setOnClickListener {
            // 선택된 라디오 버튼에 따라 현재 플레이 인원 파악
            val playerCount = when (rgPlayerCount.checkedRadioButtonId) {
                R.id.rb2Players -> 2
                R.id.rb3Players -> 3
                R.id.rb4Players -> 4
                else -> 3
            }

            // 입력된 이름 가져오기 (코틀린은 .getText().toString() 대신 .text.toString()으로 충분합니다)
            val name1 = etPlayer1.text.toString().trim()
            val name2 = etPlayer2.text.toString().trim()
            val name3 = etPlayer3.text.toString().trim()
            val name4 = etPlayer4.text.toString().trim()

            // 필수 입력 검증 (예외 처리)
            if (name1.isEmpty() || name2.isEmpty() ||
                (playerCount >= 3 && name3.isEmpty()) ||
                (playerCount == 4 && name4.isEmpty())) {
                Toast.makeText(this, "모든 플레이어의 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // 코틀린에서 리스너 람다식을 탈출하는 방법입니다
            }

            // 4. Intent를 생성하여 메인 게임 화면(GameActivity)으로 데이터 넘기기
            val intent = Intent(this, Class.forName("com.example.gogo.SessionMainActivity")).apply {
                putExtra("PLAYER_COUNT", playerCount)
                putExtra("PLAYER_NAME_1", name1)
                putExtra("PLAYER_NAME_2", name2)
                if (playerCount >= 3) putExtra("PLAYER_NAME_3", name3)
                if (playerCount == 4) putExtra("PLAYER_NAME_4", name4)
            }

            startActivity(intent)
        }

        //게임 히스토리
        btnHistories.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
