package top.outlands;

public class PrivateHolder {
    private record Record(String string){}
    private static Record record = new Record("123");
}
