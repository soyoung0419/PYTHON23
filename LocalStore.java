// File: LocalStore.java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 로컬 저장용 Store (순수 데이터만 존재)
 * - friends 리스트
 * - friendSeq 증가값
 * 
 * 이후 필요 시 Task / Schedule / Glossary 등 필드 추가 가능
 */
public class LocalStore implements Serializable {

    private static final long serialVersionUID = 1L;

    // ======== 친구 기능 ========
    public List<Friend> friends = new ArrayList<>();
    public long friendSeq = 1;

    // ===== 일기 기능 (Diary) 추가 =====
    public List<Diary> diaries = new ArrayList<>();
    public long diarySeq = 1;

    // ======== 다른 기능들 추가 가능 ========
    // public List<Task> tasks = new ArrayList<>();
    // public List<Schedule> schedules = new ArrayList<>();
}
