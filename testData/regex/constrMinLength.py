from pydantic import BaseModel, constr


class Model(BaseModel):
    abc: constr(min_length=<caret>1) = None
