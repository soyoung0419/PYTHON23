import java.time.LocalDate;
import java.util.*;

public class CalendarViewModel {

    private final DiaryDao diaries;
    

    public CalendarViewModel(DiaryDao diaries) {
        this.diaries = diaries;
        
    }

    // ─────────────────────────────────────────────
    // 일기 추가
    // ─────────────────────────────────────────────
    public Diary addDiary(LocalDate date, String text, String imagePath) {
        return diaries.addNew(date, text, imagePath);
    }

    // ─────────────────────────────────────────────
    // 특정 날짜의 일기 목록
    // ─────────────────────────────────────────────
    public List<Diary> diariesOn(LocalDate date) {
        return diaries.byDate(date);
    }


    // ─────────────────────────────────────────────
    // 전체 일기 목록 정렬 반환
    // ─────────────────────────────────────────────
    public List<Diary> getAllDiaries() {
        List<Diary> all = diaries.getAll();

        // 날짜 기준으로 정렬 (getter 사용)
        all.sort(Comparator.comparing(Diary::getDate));
        return all;
    }

    // ─────────────────────────────────────────────
    // 키워드 검색
    // ─────────────────────────────────────────────
    public List<Diary> searchKeyword(String keyword) {
        return diaries.searchByKeyword(keyword);
    }

    // ─────────────────────────────────────────────
    // 일기 수정
    // ─────────────────────────────────────────────
    public void updateDiary(long id, String newText, String newImagePath) {

        Diary origin = diaries.get(id).orElseThrow(
                () -> new IllegalArgumentException("해당 ID의 일기가 없습니다: " + id)
        );

        // 1) 텍스트 처리
        String textToApply = origin.getText();
        if (newText != null && !newText.isBlank()) {
            textToApply = newText.trim();
        }

        // 2) 이미지 처리: "__DELETE_IMAGE__" / 새 이미지 / 유지
        String imgToApply = null;

        if ("__DELETE_IMAGE__".equals(newImagePath)) {
            imgToApply = "__DELETE_IMAGE__";
        } else if (newImagePath != null && !newImagePath.isBlank()) {
            imgToApply = newImagePath;
        } else {
            // 이미지 변경 없음 → 기존 경로 유지
            imgToApply = origin.getImagePath();
        }

        // DiaryDao.update()는 Diary 엔티티 변경을 책임진다
        Diary updated = Diary.create(
                origin.getId(),
                origin.getDate(),      // 날짜는 변경 없음 (별도 기능에서 변경 가능)
                textToApply,
                imgToApply
        );

        diaries.update(updated);
    }

    // ─────────────────────────────────────────────
    // 일기 삭제
    // ─────────────────────────────────────────────
    public void deleteDiary(long id) {
        diaries.delete(id);
    }
}
