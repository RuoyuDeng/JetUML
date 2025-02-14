/*******************************************************************************
 * JetUML - A desktop application for fast UML diagramming.
 *
 * Copyright (C) 2020, 2021 by McGill University.
 *     
 * See: https://github.com/prmr/JetUML
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *******************************************************************************/
package org.jetuml.rendering.nodes;

import static org.jetuml.geom.GeomUtils.max;

import org.jetuml.diagram.DiagramElement;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.nodes.AbstractPackageNode;
import org.jetuml.geom.Dimension;
import org.jetuml.geom.Direction;
import org.jetuml.geom.Line;
import org.jetuml.geom.Point;
import org.jetuml.geom.Rectangle;
import org.jetuml.geom.Side;
import org.jetuml.rendering.DiagramRenderer;
import org.jetuml.rendering.RenderingUtils;
import org.jetuml.rendering.StringRenderer;
import org.jetuml.rendering.StringRenderer.Alignment;
import org.jetuml.rendering.StringRenderer.TextDecoration;

import javafx.scene.canvas.GraphicsContext;

/**
 * Common functionality to view the different types of package nodes.
 */
public abstract class AbstractPackageNodeRenderer extends AbstractNodeRenderer
{
	protected static final int PADDING = 10;
	protected static final int TOP_HEIGHT = 20;
	protected static final int DEFAULT_WIDTH = 100;
	protected static final int DEFAULT_BOTTOM_HEIGHT = 60;
	protected static final int DEFAULT_TOP_WIDTH = 60;
	protected static final int NAME_GAP = 3;
	private static final StringRenderer NAME_VIEWER = StringRenderer.get(Alignment.TOP_LEFT, TextDecoration.PADDED);
	
	public AbstractPackageNodeRenderer(DiagramRenderer pParent)
	{
		super(pParent);
	}
	
	@Override
	public Dimension getDefaultDimension(Node pNode)
	{
		return new Dimension(DEFAULT_WIDTH, TOP_HEIGHT + DEFAULT_BOTTOM_HEIGHT);
	}
	
	@Override
	public void draw(DiagramElement pElement, GraphicsContext pGraphics)
	{
		assert pElement instanceof AbstractPackageNode;
		Rectangle topBounds = getTopBounds((AbstractPackageNode)pElement);
		Rectangle bottomBounds = getBottomBounds((AbstractPackageNode)pElement);
		RenderingUtils.drawRectangle(pGraphics, topBounds );
		RenderingUtils.drawRectangle(pGraphics, bottomBounds );
		NAME_VIEWER.draw(((AbstractPackageNode)pElement).getName(), pGraphics, new Rectangle(topBounds.getX() + NAME_GAP, 
				topBounds.getY(), topBounds.getWidth(), topBounds.getHeight()));
	}
	
	@Override
	public Point getConnectionPoint(Node pNode, Direction pDirection)
	{
		assert pNode instanceof AbstractPackageNode;
		Rectangle topBounds = getTopBounds((AbstractPackageNode)pNode);
		Rectangle bottomBounds = getBottomBounds((AbstractPackageNode)pNode);
		Rectangle bounds = topBounds.add(bottomBounds);
		
		Point connectionPoint = super.getConnectionPoint(pNode, pDirection);
		if( connectionPoint.getY() < bottomBounds.getY() && topBounds.getMaxX() < connectionPoint.getX() )
		{
			// The connection point falls in the empty top-right corner, re-compute it so
			// it intersects the top of the bottom rectangle (basic triangle proportions)
			int delta = topBounds.getHeight() * (connectionPoint.getX() - bounds.getCenter().getX()) * 2 / 
					bounds.getHeight();
			int newX = connectionPoint.getX() - delta;
			if( newX < topBounds.getMaxX() )
			{
				newX = topBounds.getMaxX() + 1;
			}
			return new Point(newX, bottomBounds.getY());	
		}
		else
		{
			return connectionPoint;
		}
	}
	
	/*
	 * The top face of a package node is only the side of the bottom (main) rectangle.
	 */
	@Override
	public Line getFace(Node pNode, Side pSide) 
	{
		assert pNode != null && pSide != null;
		if( pSide == Side.TOP )
		{
			Rectangle topBounds = getTopBounds((AbstractPackageNode)pNode);
			Rectangle bottomBounds = getBottomBounds((AbstractPackageNode)pNode);
			return new Line(topBounds.getMaxX(), bottomBounds.getY(), bottomBounds.getMaxX(), bottomBounds.getY());
			
		}
		else if( pSide == Side.RIGHT )
		{
			return getBottomBounds((AbstractPackageNode)pNode).getSide(pSide);
		}
		else
		{
			return super.getFace(pNode, pSide);
		}
		
	}

	@Override
	protected Rectangle internalGetBounds(Node pNode)
	{
		assert pNode instanceof AbstractPackageNode;
		return getTopBounds((AbstractPackageNode)pNode).add(getBottomBounds((AbstractPackageNode)pNode));
	}
	
	/**
	 * @param pNode The package node
	 * @return The point that corresponds to the actual top right
	 *     corner of the figure (as opposed to bounds).
	 */
	public Point getTopRightCorner(AbstractPackageNode pNode)
	{
		Rectangle bottomBounds = getBottomBounds(pNode);
		return new Point(bottomBounds.getMaxX(), bottomBounds.getY());
	}
	
	
	protected Dimension getTopDimension(AbstractPackageNode pNode)
	{
		Dimension nameBounds = NAME_VIEWER.getDimension(pNode.getName());
		int topWidth = max(nameBounds.width() + 2 * NAME_GAP, DEFAULT_TOP_WIDTH);
		int topHeight = max(nameBounds.height() - 2 * NAME_GAP, TOP_HEIGHT);
		return new Dimension(topWidth, topHeight);
	}
	
	/*
	 * By default the node's top bounds is based on its position.
	 */
	protected Rectangle getTopBounds(AbstractPackageNode pNode)
	{
		Point position = pNode.position();
		Dimension topDimension = getTopDimension(pNode);
		return new Rectangle(position.getX(), position.getY(), topDimension.width(), topDimension.height());
	}
	
	protected abstract Rectangle getBottomBounds(AbstractPackageNode pNode);
}
