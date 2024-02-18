package com.company;

import com.company.objects.Man;
import com.company.utils.CopyUtils;

import java.util.*;

public class Main {

    public static void main(String[] args) {

        List<String> books = new ArrayList<>();
        books.add("book1");
        books.add("book2");
        Man man1 = new Man("Alex", 18, books);
        Man man2 = CopyUtils.deepCopy(man1);

        List<String> list1 = new ArrayList<>();
        list1.add("book1");
        list1.add("book2");
        List<String> list2 = CopyUtils.deepCopy(list1);

        List<Man> list3 = new ArrayList<>();
        list3.add(man1);
        List<Man> list4 = CopyUtils.deepCopy(list3);

        Map<String, String> map1 = new HashMap<>();
        map1.put("1", "1");
        map1.put("2", "2");
        Map<String, String> map2 = CopyUtils.deepCopy(map1);

        String[] array1 = new String[]{"1", "2"};
        String[] array2 = CopyUtils.deepCopy(array1);

        Man[] array3 = new Man[]{man1};
        Man[] array4 = CopyUtils.deepCopy(array3);

        System.out.println(man2);
        System.out.println(list2);
        System.out.println(list4);
        System.out.println(map2);
        System.out.println(Arrays.toString(array2));
        System.out.println(Arrays.toString(array4));
    }
}
