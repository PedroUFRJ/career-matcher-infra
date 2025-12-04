**INTEGRANTES: Natan Ferreira, Julia Lopes, João Pedro Bianco, Milton Salgado e Pedro Henrique.
Trabalho final da disciplina de Algoritmos e Grafos | 2025.2 | UFRJ - Universidade Federal do Rio de Janeiro**

**Título: Criando um sistema que encontra a combinação ótima e estável entre Candidatos (via dados de seus currículos) e
Vagas (dadas as descrições e requisitos) baseado em Grafos**.

O Problema dos Casamentos Estáveis
O problema dos casamentos estáveis é um clássico problema de alocação ótima e justa dadas 2 entidades.
Suponha um grafo bipartido, isto é, um grafo composto por 2 partições. Cada partição do grafo representa uma entidade,
com suas relações e cada vértice possui uma lista de prioridades em relação a cada vértice da outra partição. Por exemplo,
um Hospital pode ser representado em uma partição e os Residentes que almejam trabalhar nos hospitais podem compôr a segunda partição.
Cada residente possui uma lista em preferências dos hospitais que desejam trabalhar, assim como cada hospital possui uma lista de preferências para os candidatos.
O problema dos casamentos estáveis resolve a alocação ótima e justa neste caso, alocando os residentes nos hospitais com maior correspondência de acordo com as 
exigências dos hospitais e vice-versa.

A Solução
Esta aplicação visa se beneficiar dos casamentos estáveis realizando a alocação de vagas para candidatos (e de candidatos para vagas).
A primeira partição do grafo representa os candidatos sendo representados a partir de dados de seus currículos. Na outra partição, as vagas

são representadas dadas as suas exigências e características 

**Sobre a infraestrutura:**

Desenvolvemos uma arquitetura disponível tanto localmente, quanto na internet. 
Para disponibilizar na internet, desenvolvemos esse código que consome querys do banco e que interage com as dependências da lambda.
Esse código pega todos os dados do banco e consegue fazer filtros com base nas necessidades do front.
Um exemplo de filtro seria {
  "filters": {
    "cargo":["Compliance"]
  }
}

Além disso, ele entrega no formato necessário para consumo via requisições no link do API gateway.

TODO: Em uma arquitetura paga o banco tem que ir para a rede privada e temos que habilitar o WAF, além de possivelmente rodar o backend do candidatoescolhido sem ser localmente

TODO possível: desenvolver toda infra em IAC para ser de simples implementação.

OBS: Credenciais do banco estão escondidas no .env da lambda da própria aws


