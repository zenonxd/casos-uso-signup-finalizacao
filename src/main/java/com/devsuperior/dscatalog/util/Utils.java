package com.devsuperior.dscatalog.util;

import com.devsuperior.dscatalog.entities.Product;
import com.devsuperior.dscatalog.projections.IdProjection;
import com.devsuperior.dscatalog.projections.ProductProjection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    //pegaremos a ordenação de projection (paginação) e montar uma nova lista de Product
    //usando como a base a lista desordenada (entity)


    public static <ID> List<? extends IdProjection<ID>> replace(List<? extends IdProjection<ID>> ordered,
                                                                List<? extends IdProjection<ID>> unordered) {

        //Usar Map, pois é mais fácil para acessar os itens.
        //Long (para o id), guardaremos o product pelo ID.
        Map<ID, IdProjection<ID>> map = new HashMap<>();

        //preenchendo o Map com os elementos da lista desordenada
        for (IdProjection<ID> p : unordered) {
            map.put(p.getId(), p);
        }

        //criando lista de Produtos ordenada
        List<IdProjection<ID>> result = new ArrayList<>();

        //agora, para cada Projection da ordered (lá em cima), iremos adicionar na result
        //o produto que corresponde a Projection
        for (IdProjection<ID> p : ordered) {
            //irá para a lista, o produto que o id estiver no map, que tenha o ID do
            //objeto dentro da lista ordered
            result.add(map.get(p.getId()));
        }

        return result;
    }
}
