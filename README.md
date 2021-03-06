# LPCFG_Unsupervised_Frame_Induction

Unsupervised Frame Induction LPCFG + Embedding

To run the code, unzip the ```pop-vectors.zip``` go to ```dist``` direcory and type in 

```$ java -jar LPCFG_Unsupervised_Frame_Induction.jar ../input.feat.syntax ../pop-vectors.txt```

`input.feat.syntax` is the input file obtained from the provided frame structures and their dependency parses as one additional CATEGORICAL feature (see [1] for more). `pop-vectors.txt` is a set of embeddings based on [2].

The code will exploit all your CPU and it may require a good bit of ram (depending on the number of records and their length).
One the job finished, the resulting clustering will be written to `output.txt` which you can evaluate using scorer and gold data. We use cardinal numbers as cluster identifier and strings in the form of `R^NNN` as the identifier for Semantic roles. You can exchange these symbols by your own scripts to more meaningful labels.


For scorer see https://github.com/languagerecipes/task-2-semeval-2019/tree/master/scorer

For the gold data see https://competitions.codalab.org/competitions/19159

The output can be used for evaluation in the 3 subtasks. For subtask 2.2, you can browse the source code and choose "Merging" of semantic rols. 


[1] http://aclweb.org/anthology/S18-2016
[2] https://arxiv.org/pdf/1705.04253
