// File: FriendManager.java
// Console-only Friend Manager extracted from CampusPlannerConsole
// author: soyoung0419, expected: 친구 CRUD + 태그/검색 기능, developed: 독립 실행형 Friend 관리 프로그램

import java.io.*;
import java.nio.file.*;
import java.util.*;

// ============================================
// Utils
// ============================================
class DateTimeUtils {}

// ============================================
// Model
// ============================================
class Friend implements Serializable {
    private static final long serialVersionUID = 1L;
    final long id;
    String name;
    String uid;
    String tag;

    Friend(long id, String name, String uid) {
        this.id = id;
        this.name = name;
        this.uid = uid;
        this.tag = "null";
    }

    @Override
    public String toString() {
        return String.format("[#%d] %s (uid=%s, tag=%s)", id, name, uid, tag);
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Friend)) return false;
        return id == ((Friend)o).id;
    }

    @Override
    public int hashCode() { return Long.hashCode(id); }
}

// ============================================
// LocalStore & DB
// ============================================
class LocalStore implements Serializable {
    private static final long serialVersionUID = 1L;
    List<Friend> friends = new ArrayList<>();
    long friendSeq = 1;
}

class LocalDatabaseManager {
    private static final String FILE = "friend_data.bin";
    private static final String TMP = "friend_data.bin.tmp";

    synchronized LocalStore load() {
        File f = new File(FILE);
        if (!f.exists()) return new LocalStore();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof LocalStore) return (LocalStore) obj;
        } catch (Exception e){
            System.err.println("불러오기 실패: " + e.getMessage());
        }
        return new LocalStore();
    }

    synchronized void save(LocalStore store) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TMP))) {
            oos.writeObject(store);
        } catch (Exception e){
            System.err.println("임시 저장 실패: " + e.getMessage());
            return;
        }

        try {
            Files.move(Paths.get(TMP), Paths.get(FILE),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e){
            System.err.println("최종 교체 실패: " + e.getMessage());
        }
    }
}

// ============================================
// DAO
// ============================================

interface Crud<T> {
    T add(T t);
    Optional<T> get(long id);
    List<T> getAll();
    void update(T t);
    void delete(long id);
}

class FriendDao implements Crud<Friend> {
    private final LocalStore store;
    private final LocalDatabaseManager db;

    FriendDao(LocalStore store, LocalDatabaseManager db) {
        this.store = store;
        this.db = db;
    }

    Friend addNew(String name, String uid){
        if (!uid.matches("^uid_[0-9]+$"))
            throw new IllegalArgumentException("UID 형식 오류: uid_숫자");

        for (Friend f : store.friends)
            if (f.uid.equalsIgnoreCase(uid))
                throw new IllegalArgumentException("이미 존재하는 UID입니다.");

        long id = store.friendSeq++;
        Friend f = new Friend(id, name, uid);
        store.friends.add(f);
        db.save(store);
        return f;
    }

    @Override
    public Friend add(Friend f){
        store.friends.add(f);
        db.save(store);
        return f;
    }

    @Override
    public Optional<Friend> get(long id){
        return store.friends.stream().filter(f -> f.id == id).findFirst();
    }

    @Override
    public List<Friend> getAll(){
        return new ArrayList<>(store.friends);
    }

    @Override
    public void update(Friend f){
        get(f.id).ifPresent(old -> {
            old.name = f.name;
            old.uid = f.uid;
            db.save(store);
        });
    }

    void updateTag(long id, String newTag){
        for (Friend f : store.friends) {
            if (f.id == id) {
                f.tag = newTag;
                db.save(store);
                return;
            }
        }
        throw new IllegalArgumentException("해당 ID 없음");
    }

    List<Friend> findByTag(String tag){
        List<Friend> out = new ArrayList<>();
        for (Friend f : store.friends)
            if (f.tag != null && f.tag.equalsIgnoreCase(tag))
                out.add(f);
        return out;
    }

    @Override
    public void delete(long id){
        get(id).ifPresent(f -> {
            store.friends.remove(f);
            db.save(store);
        });
    }
}

// ============================================
// ViewModel
// ============================================

class FriendViewModel {
    private final FriendDao dao;

    FriendViewModel(FriendDao dao) {
        this.dao = dao;
    }

    Friend add(String name, String uid){
        return dao.addNew(name, uid);
    }

    List<Friend> list(){
        return dao.getAll();
    }

    void delete(long id){
        dao.delete(id);
    }

    void update(long id, String newName, String newUid){
        dao.get(id).ifPresent(f -> {
            if (newName != null && !newName.isBlank()) f.name = newName;
            if (newUid != null && !newUid.isBlank()) f.uid = newUid;
            dao.update(f);
        });
    }

    void updateTag(long id, String tag){
        dao.updateTag(id, tag);
    }

    List<Friend> findByTag(String tag){
        return dao.findByTag(tag);
    }
}

// ============================================
// Console Handler
// ============================================

public class FriendManager {

    public static void main(String[] args) {
        LocalDatabaseManager db = new LocalDatabaseManager();
        LocalStore store = db.load();
        FriendDao dao = new FriendDao(store, db);
        FriendViewModel vm = new FriendViewModel(dao);

        Scanner sc = new Scanner(System.in);

        System.out.println("=== Friend Manager ===");

        while (true) {
            System.out.println("[친구] a.추가 l.목록 u.수정 d.삭제 t.태그수정 ft.태그검색 b.종료");
            System.out.print("> ");

            String c = sc.nextLine().trim();
            if (c.equals("b")) break;

            try {
                switch (c) {
                    case "a":
                        System.out.print("이름: ");
                        String name = sc.nextLine();
                        System.out.print("UID(uid_숫자): ");
                        String uid = sc.nextLine();
                        Friend f = vm.add(name, uid);
                        System.out.println("추가됨: " + f);
                        break;

                    case "l":
                        vm.list().forEach(System.out::println);
                        break;

                    case "u":
                        System.out.print("수정할 ID: ");
                        long uidTarget = Long.parseLong(sc.nextLine());
                        System.out.print("새 이름(Enter=변경없음): ");
                        String newName = sc.nextLine();
                        System.out.print("새 UID(Enter=변경없음): ");
                        String newUid = sc.nextLine();
                        vm.update(uidTarget,
                                newName.isBlank() ? null : newName,
                                newUid.isBlank() ? null : newUid);
                        System.out.println("수정 완료");
                        break;

                    case "d":
                        System.out.print("삭제할 ID: ");
                        long del = Long.parseLong(sc.nextLine());
                        vm.delete(del);
                        System.out.println("삭제됨");
                        break;

                    case "t":
                        System.out.print("태그 수정할 ID: ");
                        long tagId = Long.parseLong(sc.nextLine());
                        System.out.print("새 태그: ");
                        String tag = sc.nextLine();
                        vm.updateTag(tagId, tag);
                        System.out.println("태그 수정 완료");
                        break;

                    case "ft":
                        System.out.print("검색할 태그: ");
                        String search = sc.nextLine();
                        List<Friend> result = vm.findByTag(search);
                        if (result.isEmpty()) System.out.println("해당 태그의 친구 없음");
                        else result.forEach(System.out::println);
                        break;

                    default:
                        System.out.println("? 알 수 없는 명령");
                }
            } catch (Exception e){
                System.out.println("오류: " + e.getMessage());
            }
        }

        db.save(store);
        System.out.println("종료됨 (저장 완료)");
    }
}
