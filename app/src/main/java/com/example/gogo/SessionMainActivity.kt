package com.example.gogo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.edit
import com.example.gogo.history.GameHistory
import com.example.gogo.history.PlayerRecord

@Suppress("UNCHECKED_CAST")
class SessionMainActivity : AppCompatActivity() {

    private var playerCount = 3
    private val playerNames = mutableListOf<String>()
    private var playerScores = HashMap<String, Int>()
    private var playerRoundChanges = HashMap<String, Int>()
    private var secondsElapsed = 0L

    private lateinit var layoutScoreBoard: LinearLayout
    private lateinit var btnStartRound: Button
    private lateinit var btnEndSession: Button

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_main)

        // 데이터 접수
        playerCount = intent.getIntExtra("PLAYER_COUNT", 3)
        secondsElapsed = intent.getLongExtra("SECONDS_ELAPSED", 0L)
        val incomingScores = intent.getSerializableExtra("PLAYER_SCORES") as? HashMap<String, Int>
        if (incomingScores != null) {
            playerScores = incomingScores
        }

        for (i in 1..playerCount) {
            val name = intent.getStringExtra("PLAYER_NAME_$i") ?: "플레이어 $i"
            if (!playerNames.contains(name)) {
                playerNames.add(name)
            }
            if (!playerScores.containsKey(name)) {
                playerScores[name] = 100
            }

        }

        // 뷰 연결
        layoutScoreBoard = findViewById(R.id.layoutScoreBoard)
        btnStartRound = findViewById(R.id.btnStartRound)
        btnEndSession = findViewById(R.id.btnEndSession)

        // 💡 3.Spinner 기반 광팔이 UI 및 인원수 체크 로직 복구
        val spinnerGwangPlayer = findViewById<Spinner>(R.id.spinnerGwangPlayer)
        val etGwangCount = findViewById<EditText>(R.id.etGwangCount)
        val gwangOptions = mutableListOf("선택 없음 (광 없음)")
        val layoutGwangPlayer = findViewById<LinearLayout>(R.id.LayoutGwangPlayer)

        if (playerCount == 4) {
            gwangOptions.addAll(playerNames)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gwangOptions)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGwangPlayer.adapter = adapter
        } else {
            // 3인, 2인 플레이 시 광팔이 설정 입력칸들을 안 보이게 날려버립니다.
            layoutGwangPlayer.visibility = View.GONE
            // 레이아웃 텍스트 가이드가 있다면 숨기기 위해 부모 레이아웃이나 타이틀을 감출 수 있습니다.
            findViewById<View>(R.id.etGwangCount).parent.let {
                if (it is View) it.visibility = View.GONE
            }
        }

        // UI 갱신 (글자 크기 17f, BOLD, 검은색, ○ 기호 적용)
        updatePlayerListUI()

        // 다음 판 시작하기 버튼 클릭 리스너
        btnStartRound.setOnClickListener {
            var gwangPlayer = ""
            var gwangCount = 0

            // 4인용일 때만 Spinner와 에디트텍스트에서 값을 가져옴
            if (playerCount == 4) {
                val selectedPos = spinnerGwangPlayer.selectedItemPosition
                if (selectedPos > 0) {
                    gwangPlayer = gwangOptions[selectedPos]
                    val gwangCountStr = etGwangCount.text.toString().trim()
                    gwangCount = if (gwangCountStr.isNotEmpty()) gwangCountStr.toInt() else 0
                }
            }

            // GameActivity로 이동하며 누적 점수 및 광팔이 데이터 전달
            val intent = Intent(this, Class.forName("com.example.gogo.GameActivity")).apply {
                putExtra("PLAYER_COUNT", playerCount)
                putExtra("GWANG_PLAYER_NAME", gwangPlayer)
                putExtra("GWANG_COUNT", gwangCount)
                putExtra("SECONDS_ELAPSED", secondsElapsed)
                putExtra("PLAYER_SCORES", playerScores)
                for (i in playerNames.indices) {
                    putExtra("PLAYER_NAME_${i + 1}", playerNames[i])
                }
            }
            startActivity(intent)
        }

        btnEndSession.setOnClickListener {
            // 1. 현재 최고 점수(우승자 점수) 찾기
            val maxScore = playerNames.maxOfOrNull { playerScores[it] ?: 100 } ?: 100

            // 2. 팝업창에 표시할 결과 메시지 생성 (우승자 강조 & 줄 바꿈으로 간격 확보)
            val resultMessage = java.lang.StringBuilder()
            for (name in playerNames) {
                val score = playerScores[name] ?: 100

                if (score == maxScore) {
                    resultMessage.append("🏆 $name : ${score}점 (승리!)\n")
                } else {
                    resultMessage.append("👤 $name : ${score}점\n")
                }
            }

            // 3. 가독성을 높인 커스텀 TextView 생성
            val customTextView = TextView(this@SessionMainActivity).apply {
                text = resultMessage.toString().trim() // 마지막 줄바꿈 제거
                textSize = 18f // 글자 크기를 시원하게 키움 (기본값보다 큼)
                typeface = android.graphics.Typeface.DEFAULT_BOLD // 글씨 굵게
                setTextColor(ContextCompat.getColor(context, android.R.color.black)) // 뚜렷한 검은색
                setPadding(60, 50, 60, 20) // 좌, 상, 우, 하 여백(Padding)을 줘서 답답하지 않게 조절
            }

            // 4. 다이얼로그(팝업창) 빌더 생성 및 뷰 적용
            AlertDialog.Builder(this@SessionMainActivity).apply {
                setTitle("📊 최종 게임 결과")
                setView(customTextView) // 기본 setMessage() 대신 위에서 예쁘게 꾸민 customTextView를 삽입

                // 5. 팝업창 내부의 '게임종료' 버튼 설정
                setPositiveButton("게임종료") { _, _ ->
                    // -------------------------------------------------------------
                    // 💾 [여기서부터 히스토리 SharedPreferences + GSON 저장 로직]
                    // -------------------------------------------------------------

                    // A. 날짜 생성 (yy.MM.dd 형식)
                    val sdf = SimpleDateFormat("yy.MM.dd", Locale.getDefault())
                    val currentDate = sdf.format(java.util.Date())

                    // B. 초 단위(secondsElapsed)를 "HH:mm" 포맷 문자열로 변환
                    val hours = secondsElapsed / 3600
                    val minutes = (secondsElapsed % 3600) / 60
                    val formattedTime = String.format("%02d:%02d", hours, minutes)

                    // C. 이번 게임의 플레이어 기록 리스트(PlayerRecord) 생성
                    val currentPlayersRecords = playerNames.map { name ->
                        val score = playerScores[name] ?: 100
                        PlayerRecord(
                            name = name,
                            finalScore = score,
                            isWinner = (score == maxScore) // 최고 점수면 true
                        )
                    }

                    // D. 최종 GameHistory 객체 완성
                    val newHistoryItem = GameHistory(
                        date = currentDate,
                        totalGameTime = formattedTime,
                        players = currentPlayersRecords
                    )

                    // E. SharedPreferences 불러오기 ("game_prefs"라는 이름의 저장소)
                    val sharedPreferences = getSharedPreferences("game_prefs", MODE_PRIVATE)
                    val gson = Gson()

                    // F. 기존에 저장되어 있던 히스토리 목록 가져오기
                    val jsonHistory = sharedPreferences.getString("history_list", null)

                    // G. 기존 기록이 없으면 새 리스트를 만들고, 있으면 원래 리스트를 코틀린 객체로 복원(디코딩)
                    val historyList: ArrayList<GameHistory> = if (jsonHistory == null) {
                        ArrayList()
                    } else {
                        val type = object : TypeToken<ArrayList<GameHistory>>() {}.type
                        gson.fromJson(jsonHistory, type)
                    }

                    // H. 복원된 리스트의 최상단(0번 인덱스)에 이번 새 게임 기록 추가 (최신순 정렬을 위해)
                    historyList.add(0, newHistoryItem)

                    // I. 새 데이터가 합쳐진 리스트 전체를 다시 JSON 텍스트로 변환(인코딩)하여 저장
                    val updatedJsonHistory = gson.toJson(historyList)
                    sharedPreferences.edit { putString("history_list", updatedJsonHistory) }

                    // -------------------------------------------------------------
                    // 데이터 저장 끝! 기존의 결과 화면 이동 및 액티비티 종료 처리
                    // -------------------------------------------------------------

                    val intent = Intent(this@SessionMainActivity, Class.forName("com.example.gogo.SetupActivity")).apply {
                        putExtra("PLAYER_COUNT", playerCount)
                        putExtra("PLAYER_SCORES", playerScores)
                        for (i in playerNames.indices) {
                            putExtra("PLAYER_NAME_${i + 1}", playerNames[i])
                        }
                    }
                    startActivity(intent)
                    finish()
                }

                // 6. 취소 버튼 설정
                setNegativeButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }

                // 팝업창 화면에 표시
                show()
            }
        }
    }


    // GameActivity에서 정산완료 버튼을 누르면 이쪽으로 데이터가 들어와 합산 연동됨
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let {
            playerCount = it.getIntExtra("PLAYER_COUNT", 3)
            secondsElapsed = it.getLongExtra("SECONDS_ELAPSED", 0L)
            val updatedScores = it.getSerializableExtra("PLAYER_SCORES") as? HashMap<String, Int>
            if (updatedScores != null) {
                playerScores = updatedScores
            }
            val changedScore = it.getSerializableExtra("PLAYER_ROUND_CHANGES") as? HashMap<String, Int>
            if (changedScore != null) {
                playerRoundChanges = changedScore
            }
            updatePlayerListUI()
        }
    }

    /**
     * 대기실의 현재 누적 점수 현황 UI를 동적으로 그려주는 함수
     */
    private fun updatePlayerListUI() {
        layoutScoreBoard.removeAllViews()
        for (name in playerNames) {
            val score = playerScores[name] ?: 100
            val changedScore = playerRoundChanges[name] ?: 0
            // 1. 가로로 배치될 하나의 줄(Row)을 만듭니다.
            val playerRowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(12, 16, 12, 16)
            }

            // 2. 이름과 현재 점수를 보여줄 왼쪽 TextView
            val nameTextView = TextView(this).apply {
                text = "👤 $name : ${score}점"
                textSize = 17f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(context, android.R.color.black))

                // 이 부분이 핵심! 남은 가로 공간을 다 차지해서 오른쪽 TextView를 끝으로 밀어냅니다.
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // 3. 변동 점수만 따로 보여줄 오른쪽 TextView (원하는 대로 다르게 꾸밀 수 있습니다!)
            val changedScoreTextView = TextView(this).apply {
                text = if (changedScore > 0) "+$changedScore" else "$changedScore" // 예: +3, -2
                textSize = 17f
                typeface = android.graphics.Typeface.DEFAULT_BOLD

                // 예시: 변동 점수가 양수면 파란색, 음수면 빨간색, 0이면 회색
                val colorRes = when {
                    changedScore > 0 -> android.R.color.holo_blue_dark
                    changedScore < 0 -> android.R.color.holo_red_dark
                    else -> android.R.color.darker_gray
                }
                setTextColor(ContextCompat.getColor(context, colorRes))
            }

            // 4. 가로줄에 두 개의 TextView를 순서대로 넣고, 보드에 추가합니다.
            playerRowLayout.addView(nameTextView)
            playerRowLayout.addView(changedScoreTextView)
            layoutScoreBoard.addView(playerRowLayout)
           /* val playerTextView = TextView(this).apply {
                // 💡 4번 수정사항: 세션 메인에서도 글자 크기를 17f로 키우고 굵게 변경
                textSize = 17f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(12, 16, 12, 16)
                // 💡 4번 수정사항: 회색 대신 가시성이 뚜렷한 검은색 적용
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                text = "👤 $name : ${score}점 ... ${changedScore}"
            }
            layoutScoreBoard.addView(playerTextView)*/
        }
    }
}
