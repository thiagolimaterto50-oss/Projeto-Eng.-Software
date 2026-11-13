package br.ufscar.tes.sgma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SessaoController {
  @GetMapping("/login")
  public String login() {
    return "login";
  }
}
