package br.ufscar.tes.sgma.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class ErroControllerAdvice {
  @ExceptionHandler({
    DataIntegrityViolationException.class,
    ObjectOptimisticLockingFailureException.class
  })
  @ResponseStatus(HttpStatus.CONFLICT)
  public String conflito(Model model) {
    model.addAttribute(
        "mensagem",
        "Não foi possível salvar: um registro duplicado ou uma alteração simultânea foi detectada."
            + " Atualize a página e confira os dados.");
    return "error";
  }
}
