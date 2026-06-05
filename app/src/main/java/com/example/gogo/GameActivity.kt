package com.example.gogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.pow

class GameActivity : AppCompatActivity() {

    private var playerCount = 3
    private val playerNames = mutableListOf<String>()
    private lateinit var originalScores: HashMap<String, Int>
    private var secondsElapsed = 0L

    // SessionMainActivity에서 넘겨받을 광팔이 데이터
    private var gwangPlayer = ""
    private var gwangCount = 0

    // 표 구성을 위한 데이터 매핑용 리스트
    private val tablePlayerNames = mutableListOf<String>() // 광팔이가 제외된 실제 게임 참여자 명단
    private val winnerCheckBoxes = mutableListOf<CheckBox>()
    private val gwangBakCheckBoxes = mutableListOf<CheckBox>()
    private val peeBakCheckBoxes = mutableListOf<CheckBox>()
    private val goBakCheckBoxes = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        playerCount = intent.getIntExtra("PLAYER_COUNT", 3)
        secondsElapsed = intent.getLongExtra("SECONDS_ELAPSED", 0L)
        originalScores = intent.getSerializableExtra("PLAYER_SCORES") as? HashMap<String, Int> ?: hashMapOf()

        for (i in 1..playerCount) {
            intent.getStringExtra("PLAYER_NAME_$i")?.let { playerNames.add(it) }
        }

        // 인텐트로부터 데이터 수집 (광팔이 선택 및 장수)
        gwangPlayer = intent.getStringExtra("GWANG_PLAYER_NAME") ?: ""
        gwangCount = intent.getIntExtra("GWANG_COUNT", 0)

        val tvGameTimer = findViewById<TextView>(R.id.tvGameTimer)
        val layoutPlayerTableContainer = findViewById<LinearLayout>(R.id.layoutPlayerTableContainer)
        val rgFirstPpuckSelect = findViewById<RadioGroup>(R.id.rgFirstPpuckSelect)
        val cbHundul = findViewById<CheckBox>(R.id.cbHundul)
        val cbBomb = findViewById<CheckBox>(R.id.cbBomb)
        val etWinnerScore = findViewById<EditText>(R.id.etWinnerScore)
        val etGoCount = findViewById<EditText>(R.id.etGoCount)
        val btnCalculatePreview = findViewById<Button>(R.id.btnCalculatePreview)
        val layoutResultContainer = findViewById<LinearLayout>(R.id.layoutResultContainer)
        val btnApplyRound = findViewById<Button>(R.id.btnApplyRound)
        val tvTotalWonDisplay = findViewById<TextView>(R.id.tvTotalWonDisplay)
        val layoutGwangInfo = findViewById<LinearLayout>(R.id.layoutGwangInfo)
        val tvGwangResult = findViewById<TextView>(R.id.tvGwangResult)

        tvGameTimer.text = "이번 판 정산 진행 중"

        // 광팔이가 존재할 경우 토스트 알림 노출
        if (playerCount == 4 && gwangPlayer.isNotEmpty() && gwangCount > 0) {
            layoutGwangInfo.visibility = View.VISIBLE
            tvGwangResult.text = "[${gwangPlayer}]님이 광을 ${gwangCount}장 팔았습니다.\n(나머지 3명에게 각각 ${gwangCount}점씩 수거, 총 +${gwangCount * 3}점 선반영)"
        } else {
            layoutGwangInfo.visibility = View.GONE
        }

        // 넘겨받은 광팔이 정보를 바탕으로 표 레이아웃 생성
        rebuildPlayerTable(layoutPlayerTableContainer)

        // 첫뻑 라디오버튼 생성
        for (name in tablePlayerNames) {
            val radioButton = RadioButton(this).apply {
                id = View.generateViewId()
                text = name
                textSize = 15f
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                layoutParams = RadioGroup.LayoutParams(0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            rgFirstPpuckSelect.addView(radioButton)
        }

        val finalRoundScores = hashMapOf<String, Int>()
        val roundChanges = hashMapOf<String, Int>()

        // 계산하기 버튼 리스너
        btnCalculatePreview.setOnClickListener {
            // 표에서 승자 찾기
            var winnerIndex = -1
            for (i in winnerCheckBoxes.indices) {
                if (winnerCheckBoxes[i].isChecked) {
                    winnerIndex = i
                    break
                }
            }

            if (winnerIndex == -1) {
                Toast.makeText(this, "승자를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val winnerName = tablePlayerNames[winnerIndex]
            val scoreStr = etWinnerScore.text.toString().trim()
            val goStr = etGoCount.text.toString().trim()

            if (scoreStr.isEmpty()) {
                Toast.makeText(this, "이긴 화투 점수를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var baseScore = scoreStr.toInt()
            val goCount = if (goStr.isNotEmpty()) goStr.toInt() else 0

            //점수 배수 계산
            //1고, 2고도 2배로 수정
            var multiplier = 1
            /*if (goCount == 1) baseScore += 1
            if (goCount == 2) baseScore += 2
            if (goCount >= 3) {
                multiplier *= 2.0.pow((goCount - 2).toDouble()).toInt()
            }*/
            multiplier *= 2.0.pow(goCount.toDouble()).toInt()

            if (cbHundul.isChecked) multiplier *= 2
            if (cbBomb.isChecked) multiplier *= 2

            val finalWinnerScore = baseScore * multiplier


            for (name in playerNames) {
                roundChanges[name] = 0
            }

            // [규칙 A] 인텐트로 받은 광팔이 정산 선반영
            if (playerCount == 4 && gwangPlayer.isNotEmpty() && gwangCount > 0) {
                for (name in playerNames) {
                    if (name == gwangPlayer) {
                        roundChanges[name] = gwangCount * 3
                    } else {
                        roundChanges[name] = -gwangCount
                    }
                }
            }

            // [규칙 B] 첫뻑 규칙 정산
            val checkedPpuckId = rgFirstPpuckSelect.checkedRadioButtonId
            val ppuckRadioButton = findViewById<RadioButton>(checkedPpuckId)
            if (ppuckRadioButton != null) {
                val ppuckPlayerName = ppuckRadioButton.text.toString()
                if (playerCount == 4) {
                    for (name in playerNames) {
                        if (name == ppuckPlayerName) roundChanges[name] = (roundChanges[name] ?: 0) + 3
                        else roundChanges[name] = (roundChanges[name] ?: 0) - 1
                    }
                } else if (playerCount == 3) {
                    for (name in playerNames) {
                        if (name == ppuckPlayerName) roundChanges[name] = (roundChanges[name] ?: 0) + 4
                        else roundChanges[name] = (roundChanges[name] ?: 0) - 2
                    }
                }
            }

            // [규칙 C] 본게임 정산 (광박, 피박, 고박 동일하게 배율 계산)
            var totalWonFromPlayers = 0

            for (i in tablePlayerNames.indices) {
                val targetName = tablePlayerNames[i]
                if (targetName == winnerName) continue

                var individualMultiplier = 1
                if (gwangBakCheckBoxes[i].isChecked) individualMultiplier *= 2
                if (peeBakCheckBoxes[i].isChecked) individualMultiplier *= 2
                if (goBakCheckBoxes[i].isChecked) individualMultiplier *= 2

                val finalLossValue = finalWinnerScore * individualMultiplier

                roundChanges[targetName] = (roundChanges[targetName] ?: 0) - finalLossValue
                totalWonFromPlayers += finalLossValue
            }

            roundChanges[winnerName] = (roundChanges[winnerName] ?: 0) + totalWonFromPlayers

            // 상단 텍스트뷰 결과 연동
            tvTotalWonDisplay.text = "🏆 승자 획득 점수: ${finalWinnerScore}점"
            tvTotalWonDisplay.visibility = View.VISIBLE

            // 프리뷰 리스트 출력
            layoutResultContainer.removeAllViews()
            for (name in playerNames) {
                val currentScore = originalScores[name] ?: 100
                val change = roundChanges[name] ?: 0
                val finalScore = currentScore + change

                finalRoundScores[name] = finalScore

                val rowTextView = TextView(this).apply {
                    textSize = 17f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, 6, 0, 6)
                }

                val changeText = if (change >= 0) "+$change" else "$change"
                if (change > 0) {
                    rowTextView.text = "○ $name: $currentScore ➡️ $finalScore ($changeText 점) ▲"
                    rowTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                } else if (change < 0) {
                    rowTextView.text = "○ $name: $currentScore ➡️ $finalScore ($changeText 점) ▼"
                    rowTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
                } else {
                    rowTextView.text = "○ $name: $currentScore ➡️ $finalScore ($changeText 점) -"
                    rowTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                }
                layoutResultContainer.addView(rowTextView)
            }
        }

        btnApplyRound.setOnClickListener {
            if (finalRoundScores.isEmpty()) {
                Toast.makeText(this, "먼저 '점수 계산하기' 버튼을 눌러 결과를 확인해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, Class.forName("com.example.gogo.SessionMainActivity")).apply {
                putExtra("PLAYER_COUNT", playerCount)
                for (i in playerNames.indices) {
                    putExtra("PLAYER_NAME_${i+1}", playerNames[i])
                }
                putExtra("PLAYER_SCORES", finalRoundScores)
                putExtra("PLAYER_ROUND_CHANGES", roundChanges)
                putExtra("SECONDS_ELAPSED", secondsElapsed)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun rebuildPlayerTable(container: LinearLayout) {
        container.removeAllViews()
        tablePlayerNames.clear()
        winnerCheckBoxes.clear()
        gwangBakCheckBoxes.clear()
        peeBakCheckBoxes.clear()
        goBakCheckBoxes.clear()

        // 광팔이를 제외한 실 참여자 필터링
        for (name in playerNames) {
            if (name == gwangPlayer) continue
            tablePlayerNames.add(name)
        }

        // 체크박스 행 생성
        for (i in tablePlayerNames.indices) {
            val name = tablePlayerNames[i]

            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)
            }

            val tvName = TextView(this).apply {
                text = name
                textSize = 15f
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
            }
            rowLayout.addView(tvName)

            // 승리 체크박스 (단일 선택)
            val cbWinner = CheckBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            cbWinner.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    for (cb in winnerCheckBoxes) {
                        if (cb != cbWinner) cb.isChecked = false
                    }
                }
            }
            rowLayout.addView(cbWinner)
            winnerCheckBoxes.add(cbWinner)

            // 광박 체크박스
            val cbGwangBak = CheckBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            rowLayout.addView(cbGwangBak)
            gwangBakCheckBoxes.add(cbGwangBak)

            // 피박 체크박스
            val cbPeeBak = CheckBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            rowLayout.addView(cbPeeBak)
            peeBakCheckBoxes.add(cbPeeBak)

            // 고박 체크박스
            val cbGoBak = CheckBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            rowLayout.addView(cbGoBak)
            goBakCheckBoxes.add(cbGoBak)

            container.addView(rowLayout)
        }

        if (winnerCheckBoxes.isNotEmpty()) winnerCheckBoxes[0].isChecked = true
    }
}
