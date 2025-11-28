import java.io.*;

public class LocalDatabaseManager {

    private static final String FILE_PATH = "campusplanner_store.bin";

    // 🔥 기본 생성자 추가
    public LocalDatabaseManager() {
    }

    /**
     * 저장된 LocalStore 객체를 파일에서 로드
     */
    public LocalStore load() {
        File f = new File(FILE_PATH);

        if (!f.exists()) {
            return new LocalStore();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof LocalStore) {
                return (LocalStore) obj;
            } else {
                return new LocalStore();
            }

        } catch (Exception e) {
            System.out.println("데이터 로드 오류: " + e.getMessage());
            return new LocalStore();
        }
    }

    /**
     * LocalStore 객체를 파일에 저장
     */
    public void save(LocalStore store) {
        if (store == null) return;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(store);
        } catch (Exception e) {
            System.out.println("데이터 저장 오류: " + e.getMessage());
        }
    }
}
