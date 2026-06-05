package com.example.gogo.history

/**
 * 하나의 게임 세션(전체 판) 기록을 담는 데이터 클래스
 */
data class GameHistory(
    val date: String,               // "26.05.21" 형식의 날짜
    val totalGameTime: String,      // "01:23" (HH:mm) 형식의 총 게임 시간
    val players: List<PlayerRecord> // 참여한 플레이어들의 기록 리스트
)

/**
 * 플레이어 개개인의 최종 점수 정보
 */
data class PlayerRecord(
    val name: String,
    val finalScore: Int,
    val isWinner: Boolean           // 🏆 최고 점수 승자 여부
)