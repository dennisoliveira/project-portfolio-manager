# 📋 Project Portfolio Manager

## 📖 Visão do Projeto

Este repositório contém a implementação do desafio técnico para vaga de **Desenvolvedor Java**.
---

## Ambiente de dev -  H2

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Ambiente de prod - PostgreSQL

```bash
docker run --name portfolio-postgres \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=portfolio \
  -p 5432:5432 -d postgres:16
  
./mvnw spring-boot:run
```

## Swagger

http://localhost:8080/swagger-ui/index.html

```
Mock: 
GERENTE: "00000000-0000-0000-0000-000000000001"
FUNCIONARIO: "00000000-0000-0000-0000-000000000002"
FUNCIONARIO: "00000000-0000-0000-0000-000000000003"
```

---

## Controle de Desenvolvimento
## 🚀 Milestones

1. [x] **Setup do projeto** — `pom.xml`, configs base (`application.yml`)
2. [x] **Entidades e migrações** — criação das tabelas via Flyway (`V1__init.sql`)
3. [x] **Serviços de domínio** — `RiskClassifier`, `StatusTransitionValidator`
4. [x] **CRUD de projetos** — operações básicas + validações
5. [x] **Transições de status** — regras de sequência e não exclusão
6. [x] **Integração API externa de membros (mock)** — criar/consultar membros
7. [x] **Associação de membros a projetos** — regras de limite (1–10) e máximo de 3 projetos ativos
8. [x] **Listagem com paginação e filtros**
9. [x] **Relatório resumido do portfólio**
10. [x] **Tratamento global de exceções**
11. [x] **Segurança (Spring Security Basic Auth)**
12. [x] **Swagger/OpenAPI documentado**
13. [x] **README final atualizado**
14. [x] **Cobertura de testes >= 30% (JaCoCo)**
15. [ ] **Cobertura de testes >= 70% (JaCoCo)**

---

## ✅ Definition of Done (DoD)

* [x] Endpoints possuem **validações** e retornam **erros padronizados**
* [x] Regras de negócio **implementadas e testadas**
* [x] API **documentada no Swagger/OpenAPI**
* [x] **Segurança básica** aplicada
* [x] **Migrações Flyway** atualizadas e consistentes
* [x] **README** atualizado com progresso e instruções de execução
* [x] Cobertura de testes **≥ 30%**
* [ ] Cobertura de testes **≥ 70%**

---

## 📊 Checklist de Progresso

### Funcionalidades

* [x] CRUD de Projetos
* [x] Status fixos e transições válidas
* [x] Regra de não exclusão (status iniciado, em andamento, encerrado)
* [x] Classificação dinâmica de risco
* [x] Integração com API externa (mockada) de membros
* [x] Associação de membros a projetos (com restrições)
* [x] Listagem com paginação e filtros
* [x] Relatório resumido do portfólio

### Não Funcionais

* [x] Arquitetura em camadas (MVC)
* [x] DTOs + mapeamento (MapStruct)
* [x] PostgreSQL + Flyway + H2 (testes)
* [x] Segurança básica (Basic Auth)
* [x] Swagger/OpenAPI ativo
* [x] Tratamento global de exceções
* [x] README atualizado
* [x] Testes unitários com cobertura ≥ 30%
* [ ] Testes unitários com cobertura ≥ 70%