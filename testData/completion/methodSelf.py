

from pydantic import BaseModel


class A(BaseModel):
    abc: str


    def test(self):
        return self.<caret>


