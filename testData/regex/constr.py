from pydantic import BaseModel, constr


class Model(BaseModel):
    abc: constr(regex='<caret>[^a-zA-Z]+') = None
