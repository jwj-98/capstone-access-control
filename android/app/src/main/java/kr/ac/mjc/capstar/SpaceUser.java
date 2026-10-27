package kr.ac.mjc.capstar;

public class SpaceUser {

    public String code;
    public String name;
    public String phoneNo;
    public String userLocation;
    public String faceImgUrl;
    //public String key;
    public String name_phoneNo;
    public int finger;

    public SpaceUser(){}

    public SpaceUser(String code, String name, String phoneNo, String userLocation, String faceImgUrl/*, String key*/, String name_phoneNo) {
        this.code = code;
        this.name = name;
        this.phoneNo = phoneNo;
        this.userLocation = userLocation;
        this.faceImgUrl = faceImgUrl;
        //this.key = key;
        this.name_phoneNo = name_phoneNo;
    }

}
