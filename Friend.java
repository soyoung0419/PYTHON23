import java.io.Serializable;
import java.util.Objects;

/**
 * Friend 엔티티
 * - id는 불변(생성 후 변경 불가)
 * - name, uid, tag는 유효성 검증을 포함한 setter 메서드 없이 도메인 동작 메서드로만 변경
 * - UID 형식 검증 (uid_숫자)
 * - null/blank 방어 로직
 */
public class Friend implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;    // 불변 값
    private String name;      // 변경 가능(도메인 규칙에 따라)
    private String uid;       // 변경 가능, 형식 검증
    private String tag;       // 선택적 태그

    // ──────────────────────────────────────
    // 생성자(private) → create() 팩토리 메서드로 생성 제한
    // ──────────────────────────────────────
    private Friend(long id, String name, String uid, String tag) {
        this.id = id;
        this.name = name;
        this.uid = uid;
        this.tag = tag;
    }

    // ──────────────────────────────────────
    // 정적 팩토리 메서드 (권장 생성 방식)
    // ──────────────────────────────────────
    public static Friend create(long id, String name, String uid) {
        validateName(name);
        validateUid(uid);
        return new Friend(id, name.trim(), uid.trim(), null);
    }

    // ──────────────────────────────────────
    // 도메인 행동 (setter 대신)
    // ──────────────────────────────────────
    public void changeName(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

    public void changeUid(String newUid) {
        validateUid(newUid);
        this.uid = newUid.trim();
    }

    public void changeTag(String newTag) {
        if (newTag == null || newTag.isBlank()) {
            this.tag = null;
        } else {
            this.tag = newTag.trim();
        }
    }

    // ──────────────────────────────────────
    // 검증 로직 (도메인 규칙 캡슐화)
    // ──────────────────────────────────────
    private static void validateName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("이름은 비어 있을 수 없습니다.");
    }

    private static void validateUid(String uid) {
        if (uid == null || uid.isBlank())
            throw new IllegalArgumentException("UID는 비어 있을 수 없습니다.");

        if (!uid.matches("^uid_\\d+$"))
            throw new IllegalArgumentException("UID 형식 오류: uid_숫자 형태여야 합니다.");
    }

    // ──────────────────────────────────────
    // Getters (필요한 값만)
    // ──────────────────────────────────────
    public long getId() { return id; }
    public String getName() { return name; }
    public String getUid() { return uid; }
    public String getTag() { return tag; }

    // ──────────────────────────────────────
    // equals & hashCode (id 기반)
    // ──────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Friend)) return false;
        Friend friend = (Friend) o;
        return id == friend.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ──────────────────────────────────────
    // toString
    // ──────────────────────────────────────
    @Override
    public String toString() {
        return String.format(
            "[#%d] %s (uid=%s, tag=%s)",
            id, name, uid, (tag == null ? "-" : tag)
        );
    }
}
