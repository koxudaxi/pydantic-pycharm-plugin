from pydantic import BaseModel


class A(BaseModel):
    abc = ""

    @classmethod
    def do(cls):
        cde = "abc"
        assert cde
