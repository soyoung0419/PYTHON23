// File: FriendManager.java
// Standalone friend management program

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FriendManager {

    /* ===================== Model ===================== */
    static class Friend implements Serializable {
        private static final long serialVersionUID = 1L;
        long id;
        String name;
        String uid;
        String tag; // optional

        Friend(long id, String name, String uid){
            this.id = id;
            this.name = name;
            this.uid = uid;
        }

        @Override
        public String toString() {
            String tagInfo = (tag != null && !tag.isBlank()) ? " [태그: " + tag + "]" : "";
            return String.format("[#%d] %s (uid=%s)%s", id, name, uid, tagInfo);
        }

        @Override public boolean equals(Object o){ return o instanceof Friend && ((Friend)o).id == id; }
        @Override public int hashCode(){ return Long.hashCode(id); }
    }

    /* ===================== Local Store ===================== */
    static class LocalStore implements Serializable {
        private static final long serialVersionUID = 1L;
        List<Friend> friends = new ArrayList<>();
        long friendSeq = 1;
    }

    /* ===================== File DB ===================== */
    static class LocalDatabaseManager {
        private static final String FILE = "friend_data.bin";
        private static final String TMP = "friend_data.tmp";

        synchronized LocalStore load(){
            File f = new File(FILE);
            if (!f.exists()) return new LocalStore();
            try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))){
                Object obj = ois.readObject();
                if (obj instanceof LocalStore) return (LocalStore)obj;
            } catch(Exception e){
                System.err.println("불러오기 실패: " + e.getMessage());
            }
            return new LocalStore();
        }

        synchronized void save(LocalStore store){
            try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TMP))){
                oos.writeObject(store);
            } catch(Exception e){
                System.err.println("임시 저장 오류: " + e.getMessage());
                return;
            }
            try {
                Files.move(Paths.get(TMP), Paths.get(FILE),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch(Exception e){
                System.err.println("교체 저장 오류: " + e.getMessage());
            }
        }
    }

    /* ===================== DAO ===================== */
    static class FriendDao {
        private final LocalStore store;
        private final LocalDatabaseManager db;

        FriendDao(LocalStore store, LocalDatabaseManager db){
            this.store = store;
            this.db = db;
        }

        Friend addNew(String name, String uid){
            if (!uid.matches("^uid_[0-9]+$"))
                throw new IllegalArgumentException("UID 형식 오류 (uid_숫자)");

            for (Friend f : store.friends)
                if (f.uid.equalsIgnoreCase(uid))
                    throw new IllegalArgumentException("UID 중복됨");

            Friend f = new Friend(store.friendSeq++, name, uid);
            store.friends.add(f);
            db.save(store);
            return f;
        }

        List<Friend> all(){
            return new ArrayList<>(store.friends);
        }

        void delete(long id){
            store.friends.removeIf(f -> f.id == id);
            db.save(store);
        }

        void update(long id, String newName, String newUid){
            for (Friend f : store.friends){
                if (f.id == id){
                    if (newName != null && !newName.isBlank())
                        f.name = newName;

                    if (newUid != null && !newUid.isBlank()){
                        if (!newUid.matches("^uid_[0-9]+$"))
                            throw new IllegalArgumentException("UID 형식 오류");

                        for (Friend other : store.friends)
                            if (other.uid.equalsIgnoreCase(newUid) && other.id != id)
                                throw new IllegalArgumentException("이미 사용 중인 UID");

                        f.uid = newUid;
                    }

                    db.save(store);
                    return;
                }
            }
            throw new IllegalArgumentException("해당 ID 없음");
        }

        void updateTag(long id, String tag){
            for (Friend f : store.friends){
                if (f.id == id){
                    f.tag = tag;
                    db.save(store);
                    return;
                }
            }
            throw new IllegalArgumentException("해당 ID 없음");
        }

        List<Friend> findByTag(String tag){
            List<Friend> result = new ArrayList<>();
            for (Friend f : store.friends)
                if (f.tag != null && f.tag.equalsIgnoreCase(tag))
                    result.add(f);
            return result;
        }

        List<Friend> findByKeyword(String keyword){
            List<Friend> list = new ArrayList<>();
            for (Friend f : store.friends){
                if (f.name.contains(keyword) || f.uid.contains(keyword))
                    list.add(f);
            }
            return list;
        }
    }

    /* ===================== ViewModel ===================== */
    static class FriendViewModel {
        private final FriendDao dao;

        FriendViewModel(FriendDao dao){
            this.dao = dao;
        }

        Friend add(String name, String uid){ return dao.addNew(name, uid); }
        List<Friend> list(){ return dao.all(); }
        void delete(long id){ dao.delete(id); }
        void update(long id, String newName, String newUid){ dao.update(id, newName, newUid); }
        void updateTag(long id, String tag){ dao.updateTag(id, tag); }
        List<Friend> findByTag(String tag){ return dao.findByTag(tag); }
        List<Friend> findKeyword(String key){ return dao.findByKeyword(key); }
    }

    /* ===================== Console UI ===================== */
    static void handleFriends(Scanner sc, FriendViewModel vm){
        while(true){
            System.out.println("\n[친구] a.추가 l.목록 u.수정 d.삭제 t.태그수정 ft.태그검색 k.검색 b.뒤로");
            System.out.print("> ");
            String c = sc.nextLine().trim();
            if (c.equals("b")) return;

            try {
                switch(c){
                    case "a":
                        System.out.print("이름: ");
                        String name = sc.nextLine();
                        System.out.print("UID(uid_숫자): ");
                        String uid = sc.nextLine();
                        System.out.println("추가됨 → " + vm.add(name, uid));
                        break;

                    case "l":
                        vm.list().forEach(System.out::println);
                        break;

                    case "u":
                        System.out.print("수정할 ID: ");
                        long id = Long.parseLong(sc.nextLine());
                        System.out.print("새 이름(Enter=유지): ");
                        String newName = sc.nextLine();
                        System.out.print("새 UID(uid_숫자, Enter=유지): ");
                        String newUid = sc.nextLine();
                        vm.update(id, newName.isBlank()? null : newName,
                                      newUid.isBlank()? null : newUid);
                        System.out.println("수정됨");
                        break;

                    case "d":
                        System.out.print("삭제할 ID: ");
                        vm.delete(Long.parseLong(sc.nextLine()));
                        System.out.println("삭제됨");
                        break;

                    case "t":
                        System.out.print("태그 수정할 ID: ");
                        long tagId = Long.parseLong(sc.nextLine());
                        System.out.print("새 태그: ");
                        vm.updateTag(tagId, sc.nextLine());
                        System.out.println("태그 수정됨");
                        break;

                    case "ft":
                        System.out.print("검색할 태그: ");
                        vm.findByTag(sc.nextLine()).forEach(System.out::println);
                        break;

                    case "k":
                        System.out.print("키워드(이름/UID): ");
                        vm.findKeyword(sc.nextLine()).forEach(System.out::println);
                        break;

                    default:
                        System.out.println("? 알 수 없는 명령");
                }
            } catch(Exception e){
                System.out.println("오류: " + e.getMessage());
            }
        }
    }

    /* ===================== Main ===================== */
    public static void main(String[] args){
        System.out.println("=== FriendManager (Standalone) ===");

        LocalDatabaseManager db = new LocalDatabaseManager();
        LocalStore store = db.load();
        FriendDao dao = new FriendDao(store, db);
        FriendViewModel vm = new FriendViewModel(dao);

        try (Scanner sc = new Scanner(System.in)){
            while(true){
                System.out.println("\n1.친구관리  0.종료");
                System.out.print("> ");
                String c = sc.nextLine().trim();

                if (c.equals("0")) break;
                else if (c.equals("1")) handleFriends(sc, vm);
                else System.out.println("? 명령어");
            }
        }

        db.save(store);
        System.out.println("종료됨 (저장 완료)");
    }
}
