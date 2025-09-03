package br.com.dio.storefront.controller;


import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("products")
@AllArgsConstructor


public class ProductController {

     @GetMapping("test")
     void test(){

        System.out.println("Test method called");

    }



}
