package Model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public abstract class Character {

    private final String _name;

    private Image _image;

    private final String _description;

    private boolean _nocturnal = false;

    private PowerEnum _power = PowerEnum.Constant;

    public Character(String name, String description, boolean isNocturnal, PowerEnum powerType, String imagePath) {
        _name = name;
        _description = description;
        _nocturnal = isNocturnal;
        _power = powerType;

        //Handling image
        File file = new File(imagePath);

        try {
            _image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return _name;
    }

    public Image getImage() {
        return _image;
    }

    public String getDescription() {
        return _description;
    }

    public boolean isNocturnal() {
        return _nocturnal;
    }

    public void changeNocturnal() {
        _nocturnal = !_nocturnal;
    }

    public PowerEnum getPower() {
        return _power;
    }

    public void setPower(PowerEnum power) {
        _power = power;
    }
}
