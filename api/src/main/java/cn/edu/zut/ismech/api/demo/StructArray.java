package cn.edu.zut.ismech.api.demo;

import java.util.Arrays;

public class StructArray {
    public static void main(String[] args) {
//        数组
//        int[] a=new int[5];
//        a[0]=10;
//        System.out.println(a.length);
        //栈 斐波那契数列

        fibonacci(10);
    }
    public  static  long fibonacci(int n) {
        if (n < 1) {
            return -1;
        }
        if (n == 1 || n == 2) {
            return 1;
        }
        long[] arr = new long[n];
        arr[0] = arr[1] = 1;		//第一个和第二个数据特殊处理
        for (int i = 2; i < n; i++) {
            arr[i] = arr[i -2] + arr[i - 1];
        }
        //可以得到整个的数列数据 仅n>2
        System.out.println("数组内容：" + Arrays.toString(arr));
        return arr[n - 1];
    }
}
