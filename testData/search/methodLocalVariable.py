from pydantic import BaseModel


class A(BaseModel):
    abc = ""

    @classmethod
    def do(cls):
        a<caret>bc = "abc"
        assert abc # expected
        ## count: 2
