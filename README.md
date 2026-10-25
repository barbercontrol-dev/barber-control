# BarberControl

Sistema de gerenciamento para barbearias.

## Cadastro e operação

- O cadastro inicial cria a conta da barbearia com nome do estabelecimento, responsável, email e senha. Somente o responsável possui acesso ao sistema.
- Depois de entrar, use **Barbeiros** para cadastrar profissionais com nome, telefone e CPF (eles não recebem login). Os serviços e agendamentos são associados à barbearia.
- Em **Agendamentos**, selecione um barbeiro para consultar ou criar horários. A opção **Todas as agendas** permite consultar todos os agendamentos da barbearia.
- O agendamento público lista os barbeiros cadastrados e apresenta os serviços da barbearia do profissional escolhido.

## API relevante

- `POST /auth/registrar`: recebe `nomeBarbearia`, `nomeResponsavel`, `email`, `senha` e `consentimentoLgpd`.
- `GET /api/barbeiros` e `POST /api/barbeiros`: listam e cadastram barbeiros da barbearia autenticada.
- `GET /api/agendamentos?barbeiroId={id}`: lista os agendamentos do barbeiro escolhido; sem o parâmetro lista a barbearia inteira.
- Os serviços criados após esta atualização ficam associados à barbearia autenticada. Serviços antigos sem `barbearia_id` precisam ser atribuídos à barbearia correta antes de voltarem a aparecer; não são compartilhados automaticamente entre contas.
- Para migrar serviços antigos, identifique o ID do responsável em `usuario` e associe os serviços legados correspondentes em `servico` preenchendo `barbearia_id`.
