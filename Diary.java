import java.io.Serializable;
import java.time.LocalDate;

public class Diary implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;          // 불변 식별자
    private LocalDate date;         // 일기 날짜
    private String text;            // 본문
    private String imagePath;       // 첨부 이미지 경로 (nullable)

    // ─────────────────────────────
    // 생성자 (private → 팩토리 메서드 사용)
    // ─────────────────────────────
    private Diary(long id, LocalDate date, String text, String imagePath) {
        this.id = id;
        this.date = date;
        this.text = text;
        this.imagePath = imagePath;
    }

    // ─────────────────────────────
    // 정적 팩토리 메서드 (권장 생성 방식)
    // ─────────────────────────────
    public static Diary create(long id, LocalDate date, String text, String imagePath) {
        if (id <= 0)
            throw new IllegalArgumentException("ID는 1 이상이어야 합니다.");

        if (date == null)
            throw new IllegalArgumentException("날짜는 null일 수 없습니다.");

        if (text == null || text.isBlank())
            throw new IllegalArgumentException("일기 본문은 비워둘 수 없습니다.");

        return new Diary(id, date, text.trim(), normalizePath(imagePath));
    }

    // 이미지 경로 공백/null 통합 처리
    private static String normalizePath(String p) {
        return (p == null || p.isBlank()) ? null : p.trim();
    }

    // ─────────────────────────────
    // 도메인 동작 (수정 메서드)
    // ─────────────────────────────
    public void changeText(String newText) {
        if (newText == null || newText.isBlank())
            throw new IllegalArgumentException("본문은 비워둘 수 없습니다.");

        this.text = newText.trim();
    }

    public void changeDate(LocalDate newDate) {
        if (newDate == null)
            throw new IllegalArgumentException("날짜는 null일 수 없습니다.");

        this.date = newDate;
    }

    public void changeImage(String newPath) {
        this.imagePath = normalizePath(newPath);
    }

    public void removeImage() {
        this.imagePath = null;
    }

    // ─────────────────────────────
    // Getter
    // ─────────────────────────────
    public long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getText() {
        return text;
    }

    public String getImagePath() {
        return imagePath;
    }

    // ─────────────────────────────
    // 출력용
    // ─────────────────────────────
    @Override
    public String toString() {
        String imgInfo = (imagePath != null) ? " (이미지 첨부)" : "";
        return String.format("[#%d] %s | %s%s", id, date, text, imgInfo);
    }

    // ─────────────────────────────
    // 동등성 비교 (ID 기준)
    // ─────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Diary)) return false;
        return id == ((Diary) o).id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
