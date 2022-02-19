package uwaterloo.cs446group7.increpeable.objects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;
import org.bson.types.ObjectId;
import android.widget.ImageView;
import uwaterloo.cs446group7.increpeable.R;

public class User extends RealmObject {
    @PrimaryKey
    private ObjectId _id = new ObjectId();
    @Required
    private String username;
    @Required
    private String password;
    @Required
    private String description;
    private ImageView userImage;

    public User() {}
    public User(String name, String password, String description) {
        username = name;
        this.password = password;
        this.description = description;
        userImage.setImageResource(R.drawable.default_user_image_background);
    }
    public String getName() { return username; }
    public String getDescription() { return description; }
    public ImageView getUserImage() { return userImage; }
    public boolean setUserImage(ImageView newImage) {
        userImage = newImage;
        return true;
    }
    public boolean changePassword(String oldPassword, String newPassword) {
        if (oldPassword.equals(password)) {
            password = newPassword;
            return true;
        }
        return false;
    }
}
