import java.io.*;
import java.nio.file.*;
import java.util.*;

// ======================= FriendManager ==========================
public class FriendManager {

    // ===== Local Persistence Structure =====
    static class LocalStore implements Serializable {
        private static final long serialVersionUID = 1L;
        List<Friend> friends = new ArrayList<>();
        long friendSeq = 1;
    }

    // ===== File DB =====
    static class LocalDatabaseManager {
        private static final String FILE = "friend_data.bin";
        private static final String TMP = "friend_data.tmp";

        synchronized LocalStore load() {
            File f = new File(FILE);
            if (!f.exists()) return new LocalStore();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Object obj = ois.readObject();
                if (obj instanceof LocalStore) return (LocalStore) obj;
            } catch (Exception e) {
                System.err.println("불러오기 실패: " + e.getMessage());
            }
            return new LocalStore();
        }

        synchronized void save(LocalStore store) {
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
            } catch (IOException e){
                System.err.println("교체 저장 오류:" + e.getMessage());
            }
        }
    }

    // ===== Model =====
    static class Friend implements Serializable {
        private static final long serialVersionUID = 1L;
        long id;
        String name;
        String uid;
        String tag;   // 추가된 필드

        Friend(long id, String name, String uid){
            this.id = id;
            this.name = name;
            this.uid = uid;
            this.tag = "null"; // 기본 태그 없음
        }

        @Override public String toString(){
            return String.format("[#%d] %s (uid=%s, tag=%s)", id, name, uid, tag);
        }
    }

    // ===== DAO =====
    static class FriendDao {
        private final LocalStore store;
        private final LocalDatabaseManager db;

        FriendDao(LocalStore store, LocalDatabaseManager db){
            this.store = store; this.db = db;
        }

        Friend addNew(String name, String uid){
            if (!uid.matches("^uid_[0-9]+$"))
                throw new IllegalArgumentException("UID 형식 오류: uid_123 형태여야 합니다.");

            for (Friend f : store.friends)
                if (f.uid.equalsIgnoreCase(uid))
                    throw new IllegalArgumentException("이미 존재하는 UID입니다.");

            Friend f = new Friend(store.friendSeq++, name, uid);
            store.friends.add(f);
            db.save(store);
            return f;
        }

        List<Friend> all(){ return new ArrayList<>(store.friends); }

        void delete(long id){
            store.friends.removeIf(f -> f.id == id);
            db.save(store);
        }

        // 기존 수정 기능
        void update(long id, String newName, String newUid) {
            for (Friend f : store.friends) {
                if (f.id == id) {

                    if (newName != null && !newName.isBlank())
                        f.name = newName;

                    if (newUid != null && !newUid.isBlank()) {

                        if (!newUid.matches("^uid_[0-9]+$"))
                            throw new IllegalArgumentException("UID 형식: uid_숫자");

                        for (Friend other : store.friends)
                            if (other.uid.equalsIgnoreCase(newUid) && other.id != id)
                                throw new IllegalArgumentException("이미 사용 중인 UID입니다.");

                        f.uid = newUid;
                    }

                    db.save(store);
                    return;
                }
            }
            throw new IllegalArgumentException("해당 ID의 친구가 없습니다.");
        }

        // 태그 변경 기능
        void updateTag(long id, String newTag) {
            for (Friend f : store.friends) {
                if (f.id == id) {
                    f.tag = newTag;
                    db.save(store);
                    return;
                }
            }
            throw new IllegalArgumentException("해당 ID의 친구가 없습니다.");
        }

        // 특정 태그 가진 친구 목록
        List<Friend> findByTag(String tag){
            List<Friend> result = new ArrayList<>();
            for (Friend f : store.friends){
                if (f.tag!= null && f.tag.equalsIgnoreCase(tag)) {
                    result.add(f);

                }
            }
                    
            return result;
        }
    }
    

    // ===== ViewModel =====
    static class FriendViewModel {
        private final FriendDao dao;

        FriendViewModel(FriendDao dao){ this.dao = dao; }

        Friend add(String name, String uid){ return dao.addNew(name, uid); }
        List<Friend> list(){ return dao.all(); }
        void delete(long id){ dao.delete(id); }
        void update(long id, String newName, String newUid){ dao.update(id, newName, newUid); }

        // 새 기능
        void updateTag(long id, String tag){ dao.updateTag(id, tag); }
        List<Friend> findByTag(String tag){ return dao.findByTag(tag); }
    }

    // ===== Main Console =====
    public static void main(String[] args) {
        LocalDatabaseManager db = new LocalDatabaseManager();
        LocalStore store = db.load();
        FriendDao dao = new FriendDao(store, db);
        FriendViewModel vm = new FriendViewModel(dao);

        try(Scanner sc = new Scanner(System.in)){
            while(true){
                System.out.println("\n[친구]  a.추가  l.목록  u.수정  d.삭제  t.태그수정  ft.태그검색  b.종료");
                System.out.print("> ");

                String c = sc.nextLine().trim();
                if(c.equals("b")) break;

                try {
                    switch(c){

                        case "a":
                        while (true) {  
                            try { 
                                System.out.print("이름: "); 
                                String name = sc.nextLine(); 
                                
                                System.out.print("UID (형식: uid_숫자): "); 
                                String uid = sc.nextLine(); 
                                
                                Friend f = vm.add(name, uid);  
                                System.out.println("추가됨: " + f); break; 
                                } catch (Exception e) { 
                                    System.out.println("오류: " + e.getMessage()); 
                                    System.out.println("다시 입력하시겠습니까? (y = 다시, n = 취소)"); 
                                    String again = sc.nextLine().trim();
                                if (!again.equalsIgnoreCase("y")) {
                                    System.out.println("친구 추가 취소됨.");
                                    break; 
                                }
                            }
                        }
                        break;

                        case "l":
                            vm.list().forEach(System.out::println);
                            break;

                        case "u":
                            System.out.print("수정할 친구 ID: ");
                            long targetId = Long.parseLong(sc.nextLine());

                            System.out.print("새 이름(Enter 시 변경 없음): ");
                            String newName = sc.nextLine();

                            System.out.print("새 UID(uid_숫자, Enter 시 변경 없음): ");
                            String newUid = sc.nextLine();

                            vm.update(targetId,
                                      newName.isBlank() ? null : newName,
                                      newUid.isBlank() ? null : newUid);

                            System.out.println("수정 완료");
                            break;

                        case "d":
                            System.out.print("삭제 ID: ");
                            long id = Long.parseLong(sc.nextLine());
                            vm.delete(id);
                            System.out.println("삭제됨");
                            break;

                        case "t":   // 태그 수정 기능
                            System.out.print("태그 수정할 ID: ");
                            long tagId = Long.parseLong(sc.nextLine());

                            System.out.print("새 태그 입력(예: 동기/팀플/선배/후배): ");
                            String tag = sc.nextLine();

                            vm.updateTag(tagId, tag);
                            System.out.println("태그 수정 완료");
                            break;

                        case "ft":  // 태그 검색 기능
                            System.out.print("검색할 태그 입력: ");
                            String searchTag = sc.nextLine();

                            List<Friend> filtered = vm.findByTag(searchTag);

                            if(filtered.isEmpty())
                                System.out.println("해당 태그의 친구가 없습니다.");
                            else
                                filtered.forEach(System.out::println);

                            break;

                        default:
                            System.out.println("? (알 수 없는 명령)");
                    }
                }   catch(Exception e){
                    System.out.println("오류: " + e.getMessage());
                }
            }
        }
        db.save(store);
        System.out.println("종료");
    }
}
