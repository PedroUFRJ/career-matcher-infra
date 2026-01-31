# Documentação referente ao banco de dados
Uma vez que o problema pode ser modelado por um grafo direcionado, de forma que existem vértices para cada objeto
com os seus atributos, é necessário tanto efetuar a modelagem do grafo quanto optar pela base de dados que armazenará isto.
Afinal, como o objetivo é se utilizar da solução dos casamentos estáveis, é obrigatório modelarmos o problema com um grafo
bipartido.

## Neo4J
Em relação à escolha do SGDB (Sistema de Gerenciamento de Banco de Dados), o escolhido foi o Neo4J que é o maior desta
categoria de SGBD's e é nativo para grafos. Benefícios para a escolha de uma base de dados em grafos incluem desde os
novos aprendizados com uma tecnologia nova à melhor forma de representação dos dados, uma vez que dados com muitos 
relacionamentos se beneficiam deste formato relativamente aos banco de dados relacionais tradicionais.

## Modelagem
Para representar o problema, será necessário um grafo bipartido de foram que o emparelhamento estável seja feito
entre os nós de cada partição

**1º Partição:**
O vértice que representa o Candidato é essencial, então vamos começar por ele. A label CANDIDATO no banco de dados possui
os seguintes atributos (ou propriedades): **_nome, telefone e e-mail_**: utilizadas para exibição das informações pessoais do candidato ao final 
do algoritmo.
    **_cargo, senioridade, país e cidade_**: utilizadas para os filtros e rankeamento dos candidatos baseadas na descrição da Vaga.

Os candidatos também possuem relações com outros vértices que serão necessários para a plena representação de um currículo.
Logo, também são precisos:
Vértice GRADUAÇÃO, com as propriedades **_curso, instituição e ano de conclusão_** que também foram selecionadas para atender
possíveis requisitos e contabilizar para o ranking. Assim, um CANDIDATO pode possuir uma relação de FORMADO_EM com GRADUAÇÃO.
Note que esta e as demais relações estão no tempo verbal passado apesar de poderem estar em curso no momento (seja uma pós
ou mesmo ou experiência ainda corrente). Mas isto não é uma limitação da representação, apenas uma escolha de simplificação. O ano de conclusão pode ser
um ano de previsão de conclusão ou, no contexto de uma experiência de trabalho corrente pode ser a data atual, de forma que apenas o recorte
do presente é relevante neste caso.
Também existem os vértices MESTRADO, DOUTORADO E PÓS-DOUTORADO com as propriedades **_curso, sub-área, instituição e ano de conclusão_** e um CANDIDATO
pode possuir uma relação de FORMADO_EM com qualquer um destes vértices. Inicialmente pode parecer exagerado esta representação se considerarmos 
as vagas comuns anunciadas na Internet, onde talvez seria necessário apenas até o MESTRADO. Mas existem muitos estágios de pesquisa ou 
desenvolvimento em empresas como Google para alunos de Doutorado, por exemplo.

A fim de representar as experiências profissionais dos CANDIDATOS, quando existem, há o vértice EXPERIÊNCIA com os atributos
**_cargo, senioridade e duração em meses_.** Assim, um CANDIDATO pode possuir uma relação de TRABALHOU_EM na EXPERIÊNCIA.

Outra seção importante tanto no currículo como nos requisitos das vagas são as habilidades. Assim, um CANDIDATO é relacionado 
com a relação de HÁBIL_EM com uma COMPETÊNCIA cujo único atributo é o _nome_.

**2º Partição:**
A protagonista desta partição é VAGA cujos atributos são: **_cargo, senioridade, modalidade e cidade_**. Enquanto que o cargo
e a senioridade são chaves para o emparelhamento com o CANDIDATO, a modalidade (presencial/híbrido/remoto) em conjunto com a cidade
e a localidade do CANDIDATO serão fatores de aumento ou decremento das preferências CANDIDATO X VAGA.
Ademais, os mesmos vértices da partição anterior ainda são mantidos aqui, apenas com a diferença nos relacionamentos
Então uma VAGA pode possuir relações de:
REQUISITA_FORMAÇÃO_EM com um ou mais vértices de GRADUAÇÃO ou MESTRADO ou DOUTORADO OU PÓS-DOUTORADO
REQUISITA_EXPERIENCIA_EM com um ou mais vértices de EXPERIÊNCIA
REQUISICA_COMPETÊNCIA_EM com um ou mais vértices de HABILIDADE
![Modelagem](./assets/modelo.png)
## Representação no Neo4J