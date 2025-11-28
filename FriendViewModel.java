import java.util.List;
import java.util.Optional;

public class FriendViewModel {

    private final FriendDao dao;

    public FriendViewModel(FriendDao dao) {
        this.dao = dao;
    }

    // ───────────────────────────────────────
    // 생성
    // ───────────────────────────────────────
    public Friend add(String name, String uid) {
        // Friend.create() + dao.addNew() 내부에서 모든 검증이 수행됨
        return dao.addNew(name, uid);
    }

    // ───────────────────────────────────────
    // 조회
    // ───────────────────────────────────────
    public List<Friend> list() {
        return dao.getAll();
    }

    public Optional<Friend> get(long id) {
        return dao.get(id);
    }

    // ───────────────────────────────────────
    // 수정
    // ───────────────────────────────────────
    public void update(long id, String newName, String newUid) {

        // ViewModel은 엔티티 직접 수정 금지 → 도메인 동작 사용
        Friend f = dao.get(id).orElseThrow(() ->
                new IllegalArgumentException("ID=" + id + " 친구를 찾을 수 없습니다.")
        );

        if (newName != null && !newName.isBlank()) {
            f.changeName(newName);
        }

        if (newUid != null && !newUid.isBlank()) {
            f.changeUid(newUid);
        }

        dao.update(f);
    }

    // ───────────────────────────────────────
    // 삭제
    // ───────────────────────────────────────
    public void delete(long id) {
        dao.delete(id);
    }

    // ───────────────────────────────────────
    // 태그 수정
    // ───────────────────────────────────────
    public void updateTag(long id, String tag) {
        dao.updateTag(id, tag);
    }

    // ───────────────────────────────────────
    // 태그 검색
    // ───────────────────────────────────────
    public List<Friend> findByTag(String tag) {
        return dao.findByTag(tag);
    }
}
