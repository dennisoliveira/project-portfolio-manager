# 📋 Project Portfolio Manager — Controle de Desenvolvimento

## 📖 Visão do Projeto

Este repositório contém a implementação do desafio técnico para vaga de **Desenvolvedor Java**.

---

## 🚀 Milestones

1. [ ] **Setup do projeto** — `pom.xml`, configs base (`application.yml`)
2. [ ] **Entidades e migrações** — criação das tabelas via Flyway (`V1__init.sql`)
3. [ ] **Serviços de domínio** — `RiskClassifier`, `StatusTransitionValidator` + testes
4. [ ] **CRUD de projetos** — operações básicas + validações
5. [ ] **Transições de status** — regras de sequência e não exclusão
6. [ ] **Integração API externa de membros (mock)** — criar/consultar membros
7. [ ] **Associação de membros a projetos** — regras de limite (1–10) e máximo de 3 projetos ativos
8. [ ] **Listagem com paginação e filtros**
9. [ ] **Relatório resumido do portfólio**
10. [ ] **Tratamento global de exceções**
11. [ ] **Segurança (Spring Security Basic Auth)**
12. [ ] **Swagger/OpenAPI documentado**
13. [ ] **Cobertura de testes >= 70% (JaCoCo)**
14. [ ] **README final atualizado**

---

## ✅ Definition of Done (DoD)

* [ ] Endpoints possuem **validações** e retornam **erros padronizados**
* [ ] Regras de negócio **implementadas e testadas**
* [ ] Cobertura de testes **≥ 70%**
* [ ] API **documentada no Swagger/OpenAPI**
* [ ] **Segurança básica** aplicada
* [ ] **Migrações Flyway** atualizadas e consistentes
* [ ] **README** atualizado com progresso e instruções de execução

---

## 📊 Checklist de Progresso

### Funcionalidades

* [ ] CRUD de Projetos
* [ ] Status fixos e transições válidas
* [ ] Regra de não exclusão (status iniciado, em andamento, encerrado)
* [ ] Classificação dinâmica de risco
* [ ] Integração com API externa (mockada) de membros
* [ ] Associação de membros a projetos (com restrições)
* [ ] Listagem com paginação e filtros
* [ ] Relatório resumido do portfólio

### Não Funcionais

* [ ] Arquitetura em camadas (MVC)
* [ ] DTOs + mapeamento (MapStruct)
* [ ] PostgreSQL + Flyway + H2 (testes)
* [ ] Segurança básica (Basic Auth)
* [ ] Swagger/OpenAPI ativo
* [ ] Tratamento global de exceções
* [ ] Testes unitários com cobertura ≥ 70%
* [ ] README atualizado