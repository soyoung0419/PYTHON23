import java.util.*;
import java.time.LocalDate;

public class DiaryDao implements Crud<Diary> {

    private final LocalStore store;
    private final LocalDatabaseManager db;

    public DiaryDao(LocalStore store, LocalDatabaseManager db) {
        this.store = store;
        this.db = db;
    }

    // ───────────────────────────────────────/
    // Diary 생성
    // ───────────────────────────────────────
    public Diary addNew(LocalDate date, String text, String imagePath) {

        // 이미지 처리(저장)
        String storedPath = null;
        try {
            storedPath = ImageHelper.saveImage(imagePath);
        } catch (Exception e) {
            throw new RuntimeException("이미지 저장 실패: " + e.getMessage());
        }

        long id = store.diarySeq++;
        Diary diary = Diary.create(id, date, text, storedPath);

        store.diaries.add(diary);
        persist();

        return diary;
    }

    // Crud.add
    @Override
    public Diary add(Diary d) {
        store.diaries.add(d);
        persist();
        return d;
    }

    @Override
    public Optional<Diary> get(long id) {
        return store.diaries.stream()
                .filter(di -> di.getId() == id)
                .findFirst();
    }

    @Override
    public List<Diary> getAll() {
        return new ArrayList<>(store.diaries);
    }

    // ───────────────────────────────────────
    // 업데이트
    // ───────────────────────────────────────
    public void update(Diary updated) {

        Diary origin = getOrFail(updated.getId());

        // 날짜
        origin.changeDate(updated.getDate());

        // 본문
        origin.changeText(updated.getText());

        // 이미지 처리
        handleImageUpdate(origin, updated.getImagePath());

        persist();
    }

    // 이미지 업데이트 규칙
    private void handleImageUpdate(Diary origin, String newImg) {

        // 1) 이미지 삭제 요청
        if ("__DELETE_IMAGE__".equals(newImg)) {
            ImageHelper.deleteImage(origin.getImagePath());
            origin.removeImage();
            return;
        }

        // 2) 새로운 이미지 저장
        if (newImg != null && !newImg.isBlank()) {
            try {
                ImageHelper.deleteImage(origin.getImagePath());
                String saved = ImageHelper.saveImage(newImg);
                origin.changeImage(saved);
            } catch (Exception e) {
                throw new RuntimeException("이미지 저장 실패: " + e.getMessage());
            }
        }
        // 3) newImg가 null → 이미지 유지 (아무 처리 X)
    }

    // ───────────────────────────────────────
    // 삭제
    // ───────────────────────────────────────
    @Override
    public void delete(long id) {
        Diary d = getOrFail(id);

        // 기존 이미지 파일 삭제
        ImageHelper.deleteImage(d.getImagePath());

        store.diaries.remove(d);
        persist();
    }

    // ───────────────────────────────────────
    // 날짜별 조회
    // ───────────────────────────────────────
    public List<Diary> byDate(LocalDate date) {
        List<Diary> res = new ArrayList<>();
        for (Diary d : store.diaries) {
            if (DateTimeUtils.isSameDay(d.getDate(), date)) {
                res.add(d);
            }
        }
        return res;
    }

    // ───────────────────────────────────────
    // 키워드 검색
    // ───────────────────────────────────────
    public List<Diary> searchByKeyword(String keyword) {
        List<Diary> result = new ArrayList<>();

        for (Diary d : store.diaries) {
            if (d.getText() != null && d.getText().contains(keyword)) {
                result.add(d);
            }
        }
        return result;
    }

    // ───────────────────────────────────────
    // 내부 유틸
    // ───────────────────────────────────────
    private Diary getOrFail(long id) {
        return get(id).orElseThrow(
                () -> new IllegalArgumentException("해당 ID(" + id + ")의 일기를 찾을 수 없습니다.")
        );
    }

    private void persist() {
        db.save(store);
    }
}
