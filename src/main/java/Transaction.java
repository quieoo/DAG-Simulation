import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Transaction {
    public Transaction(Integer b, Integer t,boolean m){
        Milestone=m;
        branch=b;
        trunk=t;
        message=randomletterstr(100);
        hash=String.format("%s%s%s%s",Milestone,branch,trunk,message).hashCode();
    }
    Integer hash;
    Integer branch;
    Integer trunk;
    String message;
    Set<Integer> approvee=new ConcurrentSkipListSet<>();
    boolean Milestone;

    public void Print(){
        System.out.printf("tx-%d, trunk-%d, brach-%d, ",hash,trunk,branch);
        System.out.println(approvee.toString());
    }

    public static String randomletterstr(int length){    //随机产生字母串
        String str="abcdefghijklmnopqrstuvwxyz";    //可随机产生的字符
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(26);            //可随机产生字符的数量
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
