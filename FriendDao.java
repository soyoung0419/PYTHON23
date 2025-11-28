import java.util.*;
import java.util.stream.Collectors;


/**
 * FriendDao
 * - Friend 엔티티에 대한 CRUD 및 태그 검색 기능 제공
 * - Friend 내부 도메인 규칙(create, changeName 등)을 활용하여 캡슐화 강화
 * - DAO는 순수 저장/조회 역할만 담당 (SRP 유지)
 */
public class FriendDao implements Crud<Friend> {

    private final LocalStore store;
    private final LocalDatabaseManager db;

    public FriendDao(LocalStore store, LocalDatabaseManager db) {
        this.store = store;
        this.db = db;
    }

    // ───────────────────────────────────────────────
    // Friend 생성
    // ───────────────────────────────────────────────
    public Friend addNew(String name, String uid) {
        ensureUidNotDuplicated(uid);

        long newId = store.friendSeq++;
        Friend friend = Friend.create(newId, name, uid);

        store.friends.add(friend);
        persist();
        return friend;
    }

    private void ensureUidNotDuplicated(String uid) {
        boolean exists = store.friends.stream()
                .anyMatch(f -> f.getUid().equalsIgnoreCase(uid));

        if (exists)
            throw new IllegalArgumentException("이미 존재하는 UID입니다: " + uid);
    }

    // ───────────────────────────────────────────────
    // CRUD
    // ───────────────────────────────────────────────
    @Override
    public Friend add(Friend f) {
        store.friends.add(f);
        persist();
        return f;
    }

    @Override
    public Optional<Friend> get(long id) {
        return store.friends.stream()
                .filter(f -> f.getId() == id)
                .findFirst();
    }

    @Override
    public List<Friend> getAll() {
        return new ArrayList<>(store.friends);
    }

    @Override
    public void update(Friend updated) {
        Friend existing = getOrFail(updated.getId());

        existing.changeName(updated.getName());
        existing.changeUid(updated.getUid());
        existing.changeTag(updated.getTag());

        persist();
    }

    @Override
    public void delete(long id) {
        Friend existing = getOrFail(id);
        store.friends.remove(existing);
        persist();
    }

    // ───────────────────────────────────────────────
    // Tag 기능
    // ───────────────────────────────────────────────
    public void updateTag(long id, String newTag) {
        Friend f = getOrFail(id);
        f.changeTag(newTag);
        persist();
    }

    public List<Friend> findByTag(String tag) {
        if (tag == null || tag.isBlank())
            return Collections.emptyList();

        return store.friends.stream()
                .filter(f -> tag.equalsIgnoreCase(f.getTag()))
                .collect(Collectors.toList());
    }

    // ───────────────────────────────────────────────
    // 내부 유틸
    // ───────────────────────────────────────────────
    private Friend getOrFail(long id) {
        return get(id).orElseThrow(() ->
                new IllegalArgumentException("ID=" + id + " 친구를 찾을 수 없습니다."));
    }

    private void persist() {
        db.save(store);
    }
}
