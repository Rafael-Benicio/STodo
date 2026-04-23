# Importante
- O App que está sendo desenvolvido é um App de Todo List
- Sempre apresente um plano de ação antes de começar as modificações


## Arquitetura
- Camadas: Controller -> Service -> Repository.
- Não injete Repositories diretamente nos Controllers.

## Code Style

- **Functions**: Keep between 4 and 20 lines; split if they are longer.
    
- **Files**: Keep under 500 lines (ideally 200-300) to avoid truncation and loss of attention.
    
- **Responsibility**: Each function must do only one thing; each module must have a single responsibility (SRP).
    
- **Naming**: Use specific and unique names; avoid generic terms like `data`, `handler`, or `Manager`.
    
- **Searchability**: Prefer names that return fewer than 5 `grep` hits across the entire project.
    
- **Typing**: Use mandatory explicit typing; do not use `any`, `Dict`, or untyped functions.
    
- **DRY**: Zero code duplication; extract shared logic into dedicated functions or modules.
    
- **Logic**: Use early returns instead of nested `if` blocks; maximum limit of 2 levels of indentation.
    
- **Errors**: Exception messages must include the offending value and the expected state.
    

## Comments & Context

- **Docstrings**: Public functions must include docstrings with the function's intent and one usage example.
    
- **Provenance**: Reference issue numbers or commit SHAs when a line exists due to a specific bug or external constraint.
    

## Tests

- **Execution**: Tests must run with a single, predictable command defined in the project.
    
- **Coverage**: Every new function must have a test; every bug fix must include a regression test.
    
- **Isolation**: Mock external I/O (API, DB, filesystem) using named fake classes, not inline stubs.
    
- **Quality**: Follow F.I.R.S.T. principles (Fast, Independent, Repeatable, Self-Validating, Timely).
    

## Dependencies & Structure

- **Injection**: Inject dependencies through constructors or parameters; avoid globals or hardcoded imports.
    
- **Abstraction**: Wrap third-party libraries behind a thin interface owned by the project.
    
- **Convention**: Strictly follow the directory conventions of the framework used (e.g., Rails, Django, Next.js).
    
- **Paths**: Use predictable path structures (e.g., `controller/model/view`, `src/lib/test`).
    

## Formatting & Logging

- **Formatting**: Use the language's default formatter (e.g., `prettier`, `gofmt`, `black`) and do not discuss style beyond that.
    
- **Logging**: Use structured JSON for debugging and observability logs; use plain text only for user-facing CLI output.